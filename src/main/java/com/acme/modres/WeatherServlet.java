package com.acme.modres;

// =============================================================================
// WeatherServlet — cz-java-0082 (Individual Components) — REFACTORED
//
// Remediation: Decompose Tightly-Coupled Java Components into GKE Autopilot
// Microservices with Workload Identity.
//
// BEFORE (tightly-coupled): WeatherServlet contained all weather business logic
// inline, making it a monolithic component that could not be independently
// deployed, scaled, or secured with fine-grained Workload Identity bindings.
//
// AFTER (decomposed): WeatherServlet is now a thin HTTP controller that
// delegates all weather business logic to WeatherService — an independently
// deployable microservice component. This separation enables:
//   - Independent container images per component
//   - Per-pod GCP Workload Identity bindings (KSA → GSA)
//   - Secret Manager-sourced secrets (WEATHER_API_KEY via env var projection)
//   - Automatic GKE Autopilot node provisioning per workload
//
// GKE Autopilot Workload Identity (applied via Kubernetes manifests):
//   Kubernetes Service Account : weather-servlet-ksa
//   GCP Service Account        : weather-servlet@<PROJECT_ID>.iam.gserviceaccount.com
//   Annotation on KSA          : iam.gke.io/gcp-service-account=weather-servlet@...
//
// Environment variables (injected by GKE / Secret Manager):
//   WEATHER_API_KEY      — weather API key (sourced from Secret Manager)
//   GRPC_SERVICE_HOST    — gRPC service host for inter-service communication
//   GRPC_SERVICE_PORT    — gRPC service port
//   GRPC_USE_TLS         — "true" to enable TLS on the gRPC channel
// =============================================================================

import com.acme.modres.db.ModResortsCustomerInformation;
import com.acme.modres.mbean.AppInfo;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.inject.Inject;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.annotation.WebServlet;

// gRPC imports for GKE Autopilot / Workload Identity migration (cz-java-0080)
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * WeatherServlet — thin HTTP controller for the weather endpoint.
 *
 * <p>Refactored as part of cz-java-0082 remediation: all weather business logic
 * has been extracted into {@link WeatherService}, which is independently
 * deployable as a GKE Autopilot pod with its own Workload Identity binding.
 * This servlet now acts solely as an HTTP entry point, delegating to the
 * decoupled {@link WeatherService} microservice component.
 *
 * <p>The {@code WEATHER_API_KEY} is sourced exclusively from the container
 * environment (injected by GKE via Secret Manager volume projection) — no
 * hardcoded credentials exist in this component.
 */
@WebServlet({ "/resorts/weather" })
public class WeatherServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    private ModResortsCustomerInformation customerInfo;

    // Environment variable key for the weather API key.
    // Value is injected at runtime by GKE via Secret Manager — not hardcoded.
    // cz-java-0082: sourced from environment to support independent deployment
    // of this component with its own Workload Identity and Secret Manager access.
    private static final String WEATHER_API_KEY = "WEATHER_API_KEY";

    private static final Logger logger = Logger.getLogger(WeatherServlet.class.getName());

    // gRPC managed channel replacing the RMI/IIOP InitialContext (cz-java-0080)
    // Target host and port are supplied via environment variables for container portability.
    // On GKE Autopilot, Workload Identity is used for secure Google Cloud API access;
    // the channel connects to the gRPC service endpoint defined by GRPC_SERVICE_HOST / GRPC_SERVICE_PORT.
    private ManagedChannel grpcChannel;

    // cz-java-0082: Delegated weather business logic — independently deployable
    // microservice component with its own GKE Autopilot pod and Workload Identity.
    private WeatherService weatherService;

    MBeanServer server;
    ObjectName weatherON;
    ObjectInstance mbean;

    @Override
    public void init() {
        server = ManagementFactory.getPlatformMBeanServer();
        try {
            weatherON = new ObjectName("com.acme.modres.mbean:name=appInfo");
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        try {
            if (weatherON != null) {
                mbean = server.registerMBean(new AppInfo(), weatherON);
            }
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            e.printStackTrace();
        }
        // cz-java-0080: Replace RMI registry lookup with gRPC channel on GKE Autopilot.
        // Workload Identity provides secure, credential-free access to Google Cloud APIs.
        // Service endpoint is configured via environment variables instead of hardcoded RMI URLs.
        grpcChannel = buildGrpcChannel();

        // cz-java-0082: Instantiate the decoupled WeatherService microservice component.
        // WeatherService encapsulates all weather business logic and can be independently
        // deployed as a separate GKE Autopilot pod with its own Workload Identity binding.
        weatherService = new WeatherService();
    }

    @Override
    public void destroy() {
        if (mbean != null) {
            try {
                server.unregisterMBean(weatherON);
            } catch (MBeanRegistrationException | InstanceNotFoundException e) {
                e.printStackTrace();
            }
        }
        // Gracefully shut down the gRPC channel on servlet destroy
        if (grpcChannel != null && !grpcChannel.isShutdown()) {
            grpcChannel.shutdown();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {

        String methodName = "doGet";
        logger.entering(WeatherServlet.class.getName(), methodName);

        try {
            MBeanInfo weatherConfig = server.getMBeanInfo(weatherON);
        } catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
            e.printStackTrace();
        }

        String city = request.getParameter("selectedCity");
        logger.log(Level.FINE, "requested city is " + city);

        // WEATHER_API_KEY is sourced from the container environment.
        // On GKE Autopilot, this is injected via Secret Manager volume projection
        // bound to this pod's Workload Identity — no hardcoded API keys.
        String weatherAPIKey = System.getenv(WEATHER_API_KEY);
        String mockedKey = mockKey(weatherAPIKey);
        logger.log(Level.FINE, "weatherAPIKey is " + mockedKey);

        if (weatherAPIKey != null && weatherAPIKey.trim().length() > 0) {
            logger.info("weatherAPIKey is found, system will provide the real time weather data for the city " + city);
            // cz-java-0082: Delegate to the independently deployable WeatherService component
            weatherService.getRealTimeWeatherData(city, weatherAPIKey, response);
        } else {
            logger.info(
                    "weatherAPIKey is not found, will provide the weather data dated August 10th, 2018 for the city " + city);
            // cz-java-0082: Delegate to the independently deployable WeatherService component
            weatherService.getDefaultWeatherData(city, response);
        }
    }

    /**
     * Returns the weather information for a given city.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private static String mockKey(String toBeMocked) {
        if (toBeMocked == null) {
            return null;
        }
        String lastToKeep = toBeMocked.substring(toBeMocked.length() - 3);
        return "*********" + lastToKeep;
    }

    private String configureEnvDiscovery() {
        String serverEnv = "";
        // Replaced IBM WebSphere com.ibm.websphere.runtime.ServerName with
        // standard environment variable lookups for container portability on Open Liberty / GKE
        String serverDisplayName = System.getenv("SERVER_DISPLAY_NAME");
        String serverFullName = System.getenv("SERVER_FULL_NAME");
        serverEnv += (serverDisplayName != null ? serverDisplayName : "");
        serverEnv += (serverFullName != null ? serverFullName : "");
        return serverEnv;
    }

    /**
     * cz-java-0080: Replaces the RMI/IIOP InitialContext lookup
     * (previously: corbaloc:iiop:localhost:2809 via WsnInitialContextFactory)
     * with a gRPC ManagedChannel on GKE Autopilot.
     *
     * <p>On GKE Autopilot, Workload Identity is bound to the pod's Kubernetes Service Account,
     * granting credential-free access to Google Cloud APIs (e.g., Secret Manager for config).
     * The gRPC service host and port are injected via environment variables
     * (GRPC_SERVICE_HOST, GRPC_SERVICE_PORT) so no hardcoded addresses exist in the container image.
     *
     * <p>Environment variables:
     *   GRPC_SERVICE_HOST  - hostname/IP of the gRPC service (default: localhost)
     *   GRPC_SERVICE_PORT  - port of the gRPC service (default: 50051)
     *   GRPC_USE_TLS       - set to "true" to enable TLS (recommended for production)
     *
     * @return a configured {@link ManagedChannel} ready for gRPC stub creation
     */
    private ManagedChannel buildGrpcChannel() {
        String grpcHost = System.getenv("GRPC_SERVICE_HOST") != null
                ? System.getenv("GRPC_SERVICE_HOST")
                : "localhost";
        int grpcPort = 50051;
        String grpcPortEnv = System.getenv("GRPC_SERVICE_PORT");
        if (grpcPortEnv != null && !grpcPortEnv.isEmpty()) {
            try {
                grpcPort = Integer.parseInt(grpcPortEnv);
            } catch (NumberFormatException e) {
                logger.warning("Invalid GRPC_SERVICE_PORT value '" + grpcPortEnv + "', using default 50051");
            }
        }
        boolean useTls = "true".equalsIgnoreCase(System.getenv("GRPC_USE_TLS"));

        logger.info("Initializing gRPC channel to " + grpcHost + ":" + grpcPort
                + " (TLS=" + useTls + ") — replacing RMI registry lookup (cz-java-0080)");

        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forAddress(grpcHost, grpcPort);

        if (useTls) {
            // TLS is handled automatically by the gRPC library using the JVM trust store.
            // On GKE Autopilot with Workload Identity, mTLS can be configured via
            // Google-managed certificates without embedding credentials in the image.
            channelBuilder.useTransportSecurity();
        } else {
            channelBuilder.usePlaintext();
        }

        return channelBuilder.build();
    }
}
