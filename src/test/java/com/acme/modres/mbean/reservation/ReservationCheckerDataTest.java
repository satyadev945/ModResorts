package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class ReservationCheckerDataTest {

    @Test
    void testConstructor() {
        ReservationList list = new ReservationList();
        ReservationCheckerData data = new ReservationCheckerData(list);
        assertEquals(list, data.getReservationList());
        assertTrue(data.isAvailible());
    }

    @Test
    void testSetSelectedDate_Valid() {
        ReservationList list = new ReservationList();
        ReservationCheckerData data = new ReservationCheckerData(list);
        boolean success = data.setSelectedDate("01/01/2023");
        assertTrue(success);
        assertNotNull(data.getSelectedDate());
    }

    @Test
    void testSetSelectedDate_Invalid() {
        ReservationList list = new ReservationList();
        ReservationCheckerData data = new ReservationCheckerData(list);
        boolean success = data.setSelectedDate("invalid-date");
        assertFalse(success);
        assertNull(data.getSelectedDate());
    }

    @Test
    void testSetAvailablility() {
        ReservationList list = new ReservationList();
        ReservationCheckerData data = new ReservationCheckerData(list);
        data.setAvailablility(false);
        assertFalse(data.isAvailible());
        data.setAvailablility(true);
        assertTrue(data.isAvailible());
    }
}
