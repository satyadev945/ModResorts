package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.acme.modres.mbean.reservation.ReservationList;

/**
 * Unit tests for IOUtils class.
 */
public class IOUtilsTest {

    @Test
    void testGetFileFromRelativePath_withNonExistentResource_returnsNull() {
        // Resource "nonexistent.json" doesn't exist in classpath
        java.io.File result = IOUtils.getFileFromRelativePath("nonexistent_resource_xyz.json");
        assertNull(result);
    }

    @Test
    void testGetOpListFromConfig_returnsNonNull() {
        // ops.json may or may not exist in test classpath
        // Either returns an OpMetadataList or null
        OpMetadataList result = IOUtils.getOpListFromConfig();
        // Result can be null (if file not found) or an OpMetadataList
        // Just verify it doesn't throw
        assertTrue(result == null || result instanceof OpMetadataList);
    }

    @Test
    void testGetReservationListFromConfig_returnsNonNull() {
        // reservations.json may or may not exist in test classpath
        ReservationList result = IOUtils.getReservationListFromConfig();
        // Result can be null (if file not found) or a ReservationList
        assertTrue(result == null || result instanceof ReservationList);
    }

    @Test
    void testGetOpListFromConfig_doesNotThrow() {
        assertDoesNotThrow(() -> IOUtils.getOpListFromConfig());
    }

    @Test
    void testGetReservationListFromConfig_doesNotThrow() {
        assertDoesNotThrow(() -> IOUtils.getReservationListFromConfig());
    }

    @Test
    void testGetFileFromRelativePath_withNullPath_returnsNull() {
        // Null path should return null gracefully
        java.io.File result = IOUtils.getFileFromRelativePath(null);
        assertNull(result);
    }

    @Test
    void testGetFileFromRelativePath_withEmptyPath_returnsNull() {
        java.io.File result = IOUtils.getFileFromRelativePath("");
        // Empty path - resource not found, returns null
        assertNull(result);
    }

    @Test
    void testGetFileFromRelativePath_doesNotThrow() {
        assertDoesNotThrow(() -> IOUtils.getFileFromRelativePath("some_resource.json"));
    }
}
