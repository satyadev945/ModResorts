package com.acme.modres;

// =============================================================================
// WeatherService — cz-java-0082 (Individual Components)
//
// Remediation: Decompose Tightly-Coupled Java Components into GKE Autopilot
// Microservices with Workload Identity.
//
// This class extracts the weather business logic that was previously embedded
// directly inside WeatherServlet (a tightly-coupled monolithic component).
// By isolating the service logic here, this component can be:
//   - Packaged into its own container image
//   - Deployed as an independent GKE Autopilot pod
//   - Bound to a dedicated GCP Workload Identity (KSA → GSA binding)
//   - Configured via Secret Manager-sourced secrets (no hardcoded credentials)
//
// GKE Autopilot Workload Identity configuration (applied via Kubernetes manifests):
//   - Kubernetes Service Account (KSA): weather-service-ksa
//   - GCP Service Account (GSA): weather-service@<PROJECT_ID>.iam.gserviceaccount.com
//   - IAM binding: roles/secretmanager.secretAccessor on weather-api-key secret
//
// Environment variables (injected by GKE / Secret Manager):
//   WEATHER_API_KEY       — API key for weather data (sourced from Secret Manager)
//   WEATHER_SERVICE_HOST  — hostname of this microservice (for inter-service calls)
//   WEATHER_SERVICE_PORT  — port of this microservice
// =============================================================================

import com.acme.modres.exception.ExceptionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * WeatherService encapsulates the weather data retrieval logic as an
 * independently deployable microservice component for GKE Autopilot.
 *
 * <p>Decomposed from {@link WeatherServlet} to resolve cz-java-0082:
 * tightly-coupled individual components reduce effectiveness in containerized
 * microservices architectures. This service can now be deployed as its own
 * GKE Autopilot pod with a dedicated Workload Identity binding, enabling
 * automatic node provisioning per workload and fine-grained IAM access to
 * Secret Manager-sourced secrets.
 *
 * <p>All configuration is sourced from environment variables — no hardcoded
 * credentials or infrastructure addresses exist in this component.
 */
public class WeatherService {

    private static final Logger logger = Logger.getLogger(WeatherService.class.getName());

    /**
     * Retrieves real-time weather data from the external weather API for the
     * specified city and writes the JSON response to the HTTP response stream.
     *
     * <p>The API key is sourced from the {@code WEATHER_API_KEY} environment
     * variable, which is injected at runtime by GKE via a Secret Manager
     * volume mount or environment variable projection — no hardcoded secrets.
     *
     * @param city       the city name to retrieve weather data for
     * @param apiKey     the weather API key (sourced from environment / Secret Manager)
     * @param response   the HTTP response to write the weather JSON into
     * @throws ServletException if a servlet-level error occurs
     * @throws IOException      if an I/O error occurs during the HTTP call
     */
    public void getRealTimeWeatherData(String city, String apiKey, HttpServletResponse response)
            throws ServletException, IOException {

        String resturl = null;
        String resturlbase = Constants.WUNDERGROUND_API_PREFIX + apiKey + Constants.WUNDERGROUND_API_PART;

        if (Constants.PARIS.equals(city)) {
            resturl = resturlbase + "France/Paris.json";
        } else if (Constants.LAS_VEGAS.equals(city)) {
            resturl = resturlbase + "NV/Las_Vegas.json";
        } else if (Constants.SAN_FRANCISCO.equals(city)) {
            resturl = resturlbase + "/CA/San_Francisco.json";
        } else if (Constants.MIAMI.equals(city)) {
            resturl = resturlbase + "FL/Miami.json";
        } else if (Constants.CORK.equals(city)) {
            resturl = resturlbase + "ireland/cork.json";
        } else if (Constants.BARCELONA.equals(city)) {
            resturl = resturlbase + "Spain/Barcelona.json";
        } else {
            String errorMsg = "Sorry, the weather information for your selected city: " + city
                    + " is not available.  Valid selections are: " + Constants.SUPPORTED_CITIES;
            ExceptionHandler.handleException(null, errorMsg, logger);
            return;
        }

        URL obj = null;
        HttpURLConnection con = null;
        try {
            obj = new URL(resturl);
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
        } catch (MalformedURLException e1) {
            String errorMsg = "Caught MalformedURLException. Please make sure the url is correct.";
            ExceptionHandler.handleException(e1, errorMsg, logger);
            return;
        } catch (ProtocolException e2) {
            String errorMsg = "Caught ProtocolException: " + e2.getMessage()
                    + ". Not able to set request method to http connection.";
            ExceptionHandler.handleException(e2, errorMsg, logger);
            return;
        } catch (IOException e3) {
            String errorMsg = "Caught IOException: " + e3.getMessage() + ". Not able to open connection.";
            ExceptionHandler.handleException(e3, errorMsg, logger);
            return;
        }

        int responseCode = con.getResponseCode();
        logger.log(Level.FINEST, "Response Code: " + responseCode);

        if (responseCode >= 200 && responseCode < 300) {
            BufferedReader in = null;
            ServletOutputStream out = null;
            try {
                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder responseStr = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
                response.setContentType("application/json");
                out = response.getOutputStream();
                out.print(responseStr.toString());
                logger.log(Level.FINE, "responseStr: " + responseStr);
            } catch (Exception e) {
                String errorMsg = "Problem occured when processing the weather server response.";
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
            String errorMsg = "REST API call " + resturl + " returns an error response: " + responseCode;
            ExceptionHandler.handleException(null, errorMsg, logger);
        }
    }

    /**
     * Retrieves default (cached) weather data for the specified city and writes
     * the JSON response to the HTTP response stream.
     *
     * <p>Used as a fallback when no {@code WEATHER_API_KEY} is available in the
     * container environment, ensuring the microservice degrades gracefully.
     *
     * @param city     the city name to retrieve default weather data for
     * @param response the HTTP response to write the weather JSON into
     * @throws ServletException if a servlet-level error occurs
     * @throws IOException      if an I/O error occurs
     */
    public void getDefaultWeatherData(String city, HttpServletResponse response)
            throws ServletException, IOException {

        DefaultWeatherData defaultWeatherData = null;
        try {
            defaultWeatherData = new DefaultWeatherData(city);
        } catch (UnsupportedOperationException e) {
            ExceptionHandler.handleException(e, e.getMessage(), logger);
            return;
        }

        ServletOutputStream out = null;
        try {
            String responseStr = defaultWeatherData.getDefaultWeatherData();
            response.setContentType("application/json");
            out = response.getOutputStream();
            out.print(responseStr);
            logger.log(Level.FINEST, "responseStr: " + responseStr);
        } catch (Exception e) {
            String errorMsg = "Problem occured when getting the default weather data.";
            ExceptionHandler.handleException(e, errorMsg, logger);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
