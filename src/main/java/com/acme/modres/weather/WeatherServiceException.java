package com.acme.modres.weather;

/**
 * Checked exception thrown by {@link WeatherService} when weather data
 * cannot be retrieved (unsupported city, API error, etc.).
 *
 * <p>Using a dedicated exception type keeps the weather microservice boundary
 * clean and allows callers (e.g. {@code WeatherServlet}) to handle weather-specific
 * failures independently of generic I/O errors.
 */
public class WeatherServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    public WeatherServiceException(String message) {
        super(message);
    }

    public WeatherServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
