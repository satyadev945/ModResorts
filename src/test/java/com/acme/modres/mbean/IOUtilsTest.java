package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import com.acme.modres.mbean.reservation.ReservationList;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

class IOUtilsTest {

    @Test
    void testGetFileFromRelativePath_NonExistent() {
        File file = IOUtils.getFileFromRelativePath("nonexistent.json");
        assertNull(file);
    }

    @Test
    void testGetOpListFromConfig_NonExistent() {
        OpMetadataList list = IOUtils.getOpListFromConfig();
        assertNull(list);
    }

    @Test
    void testGetReservationListFromConfig_NonExistent() {
        ReservationList list = IOUtils.getReservationListFromConfig();
        assertNull(list);
    }
}
