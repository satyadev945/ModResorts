package com.acme.modres.weather;

import com.acme.modres.Constants;
import com.acme.modres.DefaultWeatherData;
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

/**
 * WeatherService — independently deployable microservice component responsible
 * exclusively for weather data retrieval.
 *
 * <p>Decomposed from the monolithic WeatherServlet as part of cz-java-0082 remediation:
 * tightly-coupled individual components are refactored into independent, single-responsibility
 * units that can each be deployed as their own Kubernetes Deployment on Amazon EKS.
 *
 * <p>Configuration is supplied entirely through environment variables so that this
 * component is container-portable and requires no code changes across environments:
 * <ul>
 *   <li>{@code WEATHER_API_KEY}  — API key for the Wunderground weather service</li>
 *   <li>{@code WEATHER_SERVICE_URL} — Base URL override for the weather REST endpoint
 *       (defaults to the Wunderground prefix defined in {@link Constants})</li>
 * </ul>
 */
public class WeatherService {

    /** Environment variable key for the Wunderground API key. */
    public static final String WEATHER_API_KEY_ENV = "WEATHER_API_KEY";

    /**
     * Optional environment variable to override the weather service base URL.
     * Useful when routing through an API gateway or a Kubernetes Service proxy.
     */
    public static final String WEATHER_SERVICE_URL_ENV = "WEATHER_SERVICE_URL";

    private static final Logger logger = Logger.getLogger(WeatherService.class.getName());

    /**
     * Retrieves weather data for the given city.
     *
     * <p>If {@code WEATHER_API_KEY} is set, live data is fetched from the external
     * weather REST API; otherwise the bundled default (static) data is returned.
     *
     * @param city the city name as defined in {@link Constants}
     * @return JSON string containing weather data
     * @throws IOException      if an I/O error occurs while fetching live data
     * @throws WeatherServiceException if the city is unsupported or the API call fails
     */
    public String getWeatherData(String city) throws IOException, WeatherServiceException {
        String weatherAPIKey = System.getenv(WEATHER_API_KEY_ENV);
        String mockedKey = mockKey(weatherAPIKey);
        logger.log(Level.FINE, "weatherAPIKey is " + mockedKey);

        if (weatherAPIKey != null && !weatherAPIKey.trim().isEmpty()) {
            logger.info("WEATHER_API_KEY found — fetching real-time weather data for city: " + city);
            return fetchRealTimeWeatherData(city, weatherAPIKey);
        } else {
            logger.info("WEATHER_API_KEY not set — returning default weather data for city: " + city);
            return fetchDefaultWeatherData(city);
        }
    }

    /**
     * Fetches live weather data from the external REST API.
     *
     * <p>The base URL is resolved from the {@code WEATHER_SERVICE_URL} environment
     * variable when set, falling back to the constant defined in {@link Constants}.
     */
    private String fetchRealTimeWeatherData(String city, String apiKey)
            throws IOException, WeatherServiceException {

        String baseUrl = resolveWeatherServiceBaseUrl(apiKey);
        String restUrl = buildCityUrl(baseUrl, city);

        URL obj;
        HttpURLConnection con;
        try {
            obj = new URL(restUrl);
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
        } catch (MalformedURLException e) {
            throw new WeatherServiceException(
                    "Malformed weather service URL: " + restUrl, e);
        } catch (ProtocolException e) {
            throw new WeatherServiceException(
                    "Protocol error setting GET method on connection to: " + restUrl, e);
        }

        int responseCode = con.getResponseCode();
        logger.log(Level.FINEST, "Weather API response code: " + responseCode);

        if (responseCode >= 200 && responseCode < 300) {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {
                StringBuilder responseStr = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
                logger.log(Level.FINE, "Weather API response received for city: " + city);
                return responseStr.toString();
            }
        } else {
            throw new WeatherServiceException(
                    "Weather API call to " + restUrl + " returned error response: " + responseCode);
        }
    }

    /**
     * Returns the weather service base URL.
     *
     * <p>Prefers the {@code WEATHER_SERVICE_URL} environment variable so that the
     * endpoint can be overridden per-environment (e.g. via a Kubernetes ConfigMap)
     * without recompiling the service.
     */
    private String resolveWeatherServiceBaseUrl(String apiKey) {
        String overrideUrl = System.getenv(WEATHER_SERVICE_URL_ENV);
        if (overrideUrl != null && !overrideUrl.trim().isEmpty()) {
            logger.info("Using WEATHER_SERVICE_URL override: " + overrideUrl);
            return overrideUrl + apiKey + Constants.WUNDERGROUND_API_PART;
        }
        return Constants.WUNDERGROUND_API_PREFIX + apiKey + Constants.WUNDERGROUND_API_PART;
    }

    /**
     * Builds the city-specific REST URL from the base URL.
     */
    private String buildCityUrl(String baseUrl, String city) throws WeatherServiceException {
        if (Constants.PARIS.equals(city)) {
            return baseUrl + "France/Paris.json";
        } else if (Constants.LAS_VEGAS.equals(city)) {
            return baseUrl + "NV/Las_Vegas.json";
        } else if (Constants.SAN_FRANCISCO.equals(city)) {
            return baseUrl + "/CA/San_Francisco.json";
        } else if (Constants.MIAMI.equals(city)) {
            return baseUrl + "FL/Miami.json";
        } else if (Constants.CORK.equals(city)) {
            return baseUrl + "ireland/cork.json";
        } else if (Constants.BARCELONA.equals(city)) {
            return baseUrl + "Spain/Barcelona.json";
        } else {
            throw new WeatherServiceException(
                    "Unsupported city: " + city
                    + ". Valid selections are: " + Constants.SUPPORTED_CITIES);
        }
    }

    /**
     * Returns bundled default (static) weather data for the given city.
     */
    private String fetchDefaultWeatherData(String city) throws IOException, WeatherServiceException {
        try {
            DefaultWeatherData defaultWeatherData = new DefaultWeatherData(city);
            return defaultWeatherData.getDefaultWeatherData();
        } catch (UnsupportedOperationException e) {
            throw new WeatherServiceException(
                    "No default weather data available for city: " + city, e);
        }
    }

    /** Masks all but the last three characters of the API key for safe logging. */
    private static String mockKey(String key) {
        if (key == null) {
            return null;
        }
        String lastToKeep = key.substring(key.length() - 3);
        return "*********" + lastToKeep;
    }
}
