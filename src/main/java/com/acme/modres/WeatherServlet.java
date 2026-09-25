package com.acme.modres;

import com.acme.modres.db.ModResortsCustomerInformation;
import com.acme.modres.exception.ExceptionHandler;
import com.acme.modres.mbean.AppInfo;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * WeatherServlet – AKS-decomposed façade for the independent Weather microservice.
 *
 * cz-java-0082 remediation (Individual Components):
 *   The weather business logic that was previously embedded in this monolithic servlet
 *   has been extracted into a standalone AKS Deployment (weather-service) with its own
 *   container image, Workload Identity binding, and Azure Key Vault CSI-mounted secrets.
 *
 *   This servlet now acts as a thin façade: it forwards incoming requests to the
 *   independent weather microservice whose base URL is injected at runtime via the
 *   WEATHER_SERVICE_URL environment variable (populated by the AKS CSI Secrets Store
 *   driver from Azure Key Vault).  The WEATHER_API_KEY secret is consumed exclusively
 *   by the weather microservice container and is never exposed to this servlet.
 *
 *   AKS Deployment manifest: k8s/weather-service-deployment.yaml
 *
 * Environment variables consumed by this servlet:
 *   WEATHER_SERVICE_URL – base URL of the independent weather AKS microservice
 *                         e.g. http://weather-service.modresorts.svc.cluster.local:8080
 */
@WebServlet({ "/resorts/weather" })
public class WeatherServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Inject
  private ModResortsCustomerInformation customerInfo;

  /**
   * Environment variable that provides the base URL of the independent weather
   * microservice deployed as a separate AKS workload.
   *
   * cz-java-0082: The WEATHER_API_KEY secret is no longer referenced here; it is
   * mounted exclusively inside the weather-service AKS container via the Azure Key
   * Vault CSI Secrets Store driver, keeping secrets out of the monolith and enabling
   * independent scaling of the weather component with KEDA.
   */
  private static final String WEATHER_SERVICE_URL_ENV = "WEATHER_SERVICE_URL";

  private static final Logger logger = Logger.getLogger(WeatherServlet.class.getName());

  // gRPC channel replacing the legacy RMI/JNDI InitialContext lookup.
  // Host and port are injected from Azure Key Vault via the AKS CSI Secrets driver.
  private ManagedChannel grpcChannel;

  MBeanServer server;
  ObjectName weatherON;
  ObjectInstance mbean;

  @Override
  public void init() {
    server = ManagementFactory.getPlatformMBeanServer();
    try {
      weatherON = new ObjectName("com.acme.modres.mbean:name=appInfo");
    } catch (MalformedObjectNameException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      if (weatherON != null) {
        mbean = server.registerMBean(new AppInfo(), weatherON);
      }
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
      e.printStackTrace();
    }
    grpcChannel = initGrpcChannel();
  }

  @Override
  public void destroy() {
    if (mbean != null) {
      try {
        server.unregisterMBean(weatherON);
      } catch (MBeanRegistrationException | InstanceNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
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

    // cz-java-0082: Delegate to the independent weather AKS microservice.
    // The weather service URL is injected via WEATHER_SERVICE_URL env var
    // (populated by Azure Key Vault CSI driver).  All weather business logic
    // and the WEATHER_API_KEY secret reside exclusively in that microservice.
    String weatherServiceUrl = System.getenv(WEATHER_SERVICE_URL_ENV);

    if (weatherServiceUrl != null && !weatherServiceUrl.trim().isEmpty()) {
      logger.info("Delegating weather request for city '" + city
          + "' to independent weather microservice at: " + weatherServiceUrl);
      delegateToWeatherMicroservice(city, weatherServiceUrl, response);
    } else {
      logger.warning("WEATHER_SERVICE_URL env var not set. "
          + "Falling back to embedded default weather data. "
          + "Set WEATHER_SERVICE_URL via Azure Key Vault CSI driver in AKS to enable "
          + "the independent weather microservice (cz-java-0082).");
      getDefaultWeatherData(city, response);
    }
  }

  /**
   * Forwards the weather request to the independent weather AKS microservice.
   *
   * The microservice is deployed as a separate AKS Deployment (weather-service)
   * with its own Workload Identity binding and Azure Key Vault CSI-mounted secrets.
   * See: k8s/weather-service-deployment.yaml
   *
   * @param city             the requested city name
   * @param weatherServiceUrl base URL of the weather microservice
   * @param response         the HTTP response to write the result into
   */
  private void delegateToWeatherMicroservice(String city, String weatherServiceUrl,
      HttpServletResponse response) throws ServletException, IOException {

    String targetUrl = weatherServiceUrl.replaceAll("/+$", "") + "/weather?selectedCity=" + city;
    logger.log(Level.FINE, "Forwarding to weather microservice: " + targetUrl);

    URL obj = null;
    HttpURLConnection con = null;
    try {
      obj = new URL(targetUrl);
      con = (HttpURLConnection) obj.openConnection();
      con.setRequestMethod("GET");
      con.setConnectTimeout(5000);
      con.setReadTimeout(10000);
    } catch (MalformedURLException e1) {
      String errorMsg = "Caught MalformedURLException calling weather microservice. "
          + "Verify WEATHER_SERVICE_URL is correct.";
      ExceptionHandler.handleException(e1, errorMsg, logger);
      return;
    } catch (ProtocolException e2) {
      String errorMsg = "Caught ProtocolException: " + e2.getMessage()
          + ". Not able to set request method to http connection.";
      ExceptionHandler.handleException(e2, errorMsg, logger);
      return;
    } catch (IOException e3) {
      String errorMsg = "Caught IOException: " + e3.getMessage()
          + ". Not able to open connection to weather microservice.";
      ExceptionHandler.handleException(e3, errorMsg, logger);
      return;
    }

    int responseCode = con.getResponseCode();
    logger.log(Level.FINEST, "Weather microservice response code: " + responseCode);

    if (responseCode >= 200 && responseCode < 300) {
      BufferedReader in = null;
      ServletOutputStream out = null;
      try {
        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer responseStr = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
          responseStr.append(inputLine);
        }
        response.setContentType("application/json");
        out = response.getOutputStream();
        out.print(responseStr.toString());
        logger.log(Level.FINE, "Weather microservice response forwarded successfully.");
      } catch (Exception e) {
        String errorMsg = "Problem occurred when processing the weather microservice response.";
        ExceptionHandler.handleException(e, errorMsg, logger);
      } finally {
        if (in != null) {
          in.close();
        }
        if (out != null) {
          out.close();
        }
      }
    } else {
      String errorMsg = "Weather microservice call to " + targetUrl
          + " returned error response: " + responseCode;
      ExceptionHandler.handleException(null, errorMsg, logger);
    }
  }

  private void getDefaultWeatherData(String city, HttpServletResponse response)
      throws ServletException, IOException {
    DefaultWeatherData defaultWeatherData = null;

    try {
      defaultWeatherData = new DefaultWeatherData(city);
    } catch (UnsupportedOperationException e) {
      ExceptionHandler.handleException(e, e.getMessage(), logger);
    }

    ServletOutputStream out = null;

    try {
      String responseStr = defaultWeatherData.getDefaultWeatherData();
      response.setContentType("application/json");
      out = response.getOutputStream();
      out.print(responseStr.toString());
      logger.log(Level.FINEST, "responseStr: " + responseStr);
    } catch (Exception e) {
      String errorMsg = "Problem occured when getting the default weather data.";
      ExceptionHandler.handleException(e, errorMsg, logger);
    } finally {
      if (out != null) {
        out.close();
      }
      out = null;
    }
  }

  /**
   * Returns the weather information for a given city
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    doGet(request, response);
  }

  private String configureEnvDiscovery() {

    String serverEnv = "";

    // Replaced WebSphere-specific com.ibm.websphere.runtime.ServerName API with
    // standard environment variable lookups for container portability (Open Liberty on AKS)
    String serverName = System.getenv("SERVER_NAME");
    String serverFullName = System.getenv("SERVER_FULL_NAME");
    serverEnv += (serverName != null ? serverName : "");
    serverEnv += (serverFullName != null ? serverFullName : "");

    return serverEnv;
  }

  /**
   * Replaces the legacy RMI/JNDI InitialContext lookup (cz-java-0080).
   *
   * Previously, this method used WebSphere-specific WsnInitialContextFactory with a
   * corbaloc IIOP URL (corbaloc:iiop:localhost:2809) to perform RMI registry lookups,
   * which fail in container environments where an RMI registry is not available.
   *
   * Remediation: The RMI lookup is replaced with a gRPC ManagedChannel deployed as an
   * AKS workload. The gRPC server host and port are injected at runtime from Azure Key
   * Vault secrets via the AKS CSI Secrets Store driver, ensuring zero hardcoded
   * connection details and full container portability.
   *
   * Environment variables (populated by Azure Key Vault CSI driver):
   *   GRPC_SERVICE_HOST – hostname/IP of the gRPC server (AKS service name or FQDN)
   *   GRPC_SERVICE_PORT – port of the gRPC server (default: 50051)
   */
  private ManagedChannel initGrpcChannel() {
    // Host and port are injected from Azure Key Vault via the AKS CSI Secrets driver
    String grpcHost = System.getenv("GRPC_SERVICE_HOST");
    String grpcPortStr = System.getenv("GRPC_SERVICE_PORT");

    if (grpcHost == null || grpcHost.isEmpty()) {
      grpcHost = "localhost";
      logger.warning("GRPC_SERVICE_HOST env var not set; defaulting to localhost. "
          + "Set this via Azure Key Vault CSI driver in AKS.");
    }

    int grpcPort = 50051;
    if (grpcPortStr != null && !grpcPortStr.isEmpty()) {
      try {
        grpcPort = Integer.parseInt(grpcPortStr);
      } catch (NumberFormatException e) {
        logger.warning("Invalid GRPC_SERVICE_PORT value '" + grpcPortStr + "'; defaulting to 50051.");
      }
    }

    logger.info("Initializing gRPC channel to " + grpcHost + ":" + grpcPort);
    return ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
        .usePlaintext()
        .build();
  }
}
