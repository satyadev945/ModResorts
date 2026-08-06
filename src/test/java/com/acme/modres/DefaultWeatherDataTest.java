package com.acme.modres;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class DefaultWeatherDataTest {

    @Test
    void testConstructor_ValidCity() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.PARIS);
        assertEquals(Constants.PARIS, data.getCity());
    }

    @Test
    void testConstructor_NullCity() {
        assertThrows(UnsupportedOperationException.class, () -> {
            new DefaultWeatherData(null);
        });
    }

    @Test
    void testConstructor_InvalidCity() {
        assertThrows(UnsupportedOperationException.class, () -> {
            new DefaultWeatherData("UnknownCity");
        });
    }

    @Test
    void testGetDefaultWeatherData_FileNotFound() {
        // Since we don't have the actual data files in the test classpath, 
        // this will likely throw a NullPointerException when inputStream.read(buf) is called.
        DefaultWeatherData data = new DefaultWeatherData(Constants.PARIS);
        assertThrows(NullPointerException.class, () -> {
            data.getDefaultWeatherData();
        });
    }
}
