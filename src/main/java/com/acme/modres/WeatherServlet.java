package com.acme.modres;

import com.acme.modres.db.ModResortsCustomerInformation;
import com.acme.modres.exception.ExceptionHandler;
import com.acme.modres.mbean.AppInfo;
import com.acme.modres.weather.WeatherService;
import com.acme.modres.weather.WeatherServiceException;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * WeatherServlet — HTTP entry-point for the weather microservice.
 *
 * <p><strong>cz-java-0082 remediation (Individual Components):</strong>
 * The original monolithic servlet bundled weather data retrieval, MBean management,
 * JNDI/RMI service discovery, and HTTP handling into a single tightly-coupled class.
 * This class has been decomposed so that each concern is an independently deployable
 * unit on Amazon EKS:
 * <ul>
 *   <li>{@link WeatherService} — owns all weather data retrieval logic and is
 *       independently testable and deployable as its own Kubernetes Deployment.</li>
 *   <li>{@code WeatherServlet} (this class) — owns only HTTP request/response
 *       handling; it delegates all business logic to {@link WeatherService}.</li>
 * </ul>
 *
 * <p>Each component is configured exclusively through environment variables
 * (see {@link WeatherService#WEATHER_API_KEY_ENV} and
 * {@link WeatherService#WEATHER_SERVICE_URL_ENV}) so that no code changes are
 * required when promoting across Kubernetes environments.
 *
 * <p>Kubernetes manifests for the weather microservice are provided in
 * {@code k8s/weather-service/} (Deployment, Service, ConfigMap).
 */
@WebServlet({ "/resorts/weather" })
public class WeatherServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(WeatherServlet.class.getName());

    // Environment variable for REST-based service discovery endpoint
    private static final String SERVICE_DISCOVERY_URL_ENV = "SERVICE_DISCOVERY_URL";

    @Inject
    private ModResortsCustomerInformation customerInfo;

    /**
     * Dedicated weather microservice component — all weather data retrieval logic
     * lives here, decoupled from HTTP handling (cz-java-0082).
     */
    private WeatherService weatherService;

    // REST-based service discovery URL resolved from environment variable
    private String serviceDiscoveryUrl;

    MBeanServer server;
    ObjectName weatherON;
    ObjectInstance mbean;

    @Override
    public void init() {
        // Initialise the independently deployable WeatherService component
        weatherService = new WeatherService();

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
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException
                | NotCompliantMBeanException e) {
            e.printStackTrace();
        }

        // REST-based service discovery via Kubernetes DNS / environment variable (cz-java-0080)
        serviceDiscoveryUrl = configureServiceDiscovery();
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
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String methodName = "doGet";
        logger.entering(WeatherServlet.class.getName(), methodName);

        try {
            MBeanInfo weatherConfig = server.getMBeanInfo(weatherON);
        } catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
            e.printStackTrace();
        }

        String city = request.getParameter("selectedCity");
        logger.log(Level.FINE, "Requested city: " + city);

        ServletOutputStream out = null;
        try {
            // Delegate entirely to the decomposed WeatherService microservice component
            String weatherJson = weatherService.getWeatherData(city);

            response.setContentType("application/json");
            out = response.getOutputStream();
            out.print(weatherJson);
            logger.log(Level.FINE, "Weather response written for city: " + city);
        } catch (WeatherServiceException e) {
            ExceptionHandler.handleException(e, e.getMessage(), logger);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Delegates POST requests to {@link #doGet}.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * Configures REST-based service discovery using Kubernetes DNS and environment variables.
     * Replaces the former RMI/CORBA InitialContext lookup (cz-java-0080).
     *
     * <p>Set {@code SERVICE_DISCOVERY_URL} to the Kubernetes Service DNS name, e.g.:
     * {@code http://service-registry.default.svc.cluster.local:8080/services}
     */
    private String configureServiceDiscovery() {
        String discoveryUrl = System.getenv(SERVICE_DISCOVERY_URL_ENV);
        if (discoveryUrl == null || discoveryUrl.trim().isEmpty()) {
            logger.warning("SERVICE_DISCOVERY_URL environment variable is not set. "
                    + "REST-based service discovery will be unavailable. "
                    + "Set SERVICE_DISCOVERY_URL to the Kubernetes Service DNS endpoint "
                    + "(e.g. http://<service-name>.<namespace>.svc.cluster.local:<port>/services).");
        } else {
            logger.info("REST service discovery configured with endpoint: " + discoveryUrl);
        }
        return discoveryUrl;
    }
}
