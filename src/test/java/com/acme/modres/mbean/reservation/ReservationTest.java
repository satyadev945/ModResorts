package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void testDefaultConstructor() {
        Reservation res = new Reservation();
        assertNull(res.getFromDate());
        assertNull(res.getToDate());
    }

    @Test
    void testParameterizedConstructor() {
        Reservation res = new Reservation("2023-01-01", "2023-01-10");
        assertEquals("2023-01-01", res.getFromDate());
        assertEquals("2023-01-10", res.getToDate());
    }

    @Test
    void testSettersAndGetters() {
        Reservation res = new Reservation();
        res.setFromDate("2023-02-01");
        res.setToDate("2023-02-10");
        
        assertEquals("2023-02-01", res.getFromDate());
        assertEquals("2023-02-10", res.getToDate());
    }
}
