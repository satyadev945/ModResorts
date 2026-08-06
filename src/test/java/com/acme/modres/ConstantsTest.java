package com.acme.modres;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstantsTest {

    @Test
    void testConstantsValues() {
        assertEquals("Barcelona", Constants.BARCELONA);
        assertEquals("Cork", Constants.CORK);
        assertEquals("Miami", Constants.MIAMI);
        assertEquals("San_Francisco", Constants.SAN_FRANCISCO);
        assertEquals("Paris", Constants.PARIS);
        assertEquals("Las_Vegas", Constants.LAS_VEGAS);
    }

    @Test
    void testSupportedCities() {
        assertNotNull(Constants.SUPPORTED_CITIES);
        assertEquals(6, Constants.SUPPORTED_CITIES.length);
    }

    @Test
    void testWeatherFiles() {
        assertEquals("barcelona.json", Constants.BACELONA_WEATHER_FILE);
        assertEquals("cork.json", Constants.CORK_WEATHER_FILE);
        assertEquals("nv.json", Constants.LAS_VEGAS_WEATHER_FILE);
        assertEquals("miami.json", Constants.MIAMI_WEATHER_FILE);
        assertEquals("paris.json", Constants.PARIS_WEATHER_FILE);
        assertEquals("sanfran.json", Constants.SAN_FRANCESCO_WEATHER_FILE);
    }

    @Test
    void testApiConstants() {
        assertEquals("http://api.wunderground.com/api/", Constants.WUNDERGROUND_API_PREFIX);
        assertEquals("/forecast/geolookup/conditions/q/", Constants.WUNDERGROUND_API_PART);
        assertEquals("MM/dd/yyyy", Constants.DATA_FORMAT);
    }
}
