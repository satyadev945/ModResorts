package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for Reservation class.
 */
public class ReservationTest {

    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservation = new Reservation();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        assertNotNull(reservation);
    }

    @Test
    void testDefaultConstructor_fieldsAreNull() {
        assertNull(reservation.getFromDate());
        assertNull(reservation.getToDate());
    }

    @Test
    void testParameterizedConstructor_setsFields() {
        Reservation r = new Reservation("2024-01-01", "2024-01-10");
        assertEquals("2024-01-01", r.getFromDate());
        assertEquals("2024-01-10", r.getToDate());
    }

    @Test
    void testParameterizedConstructor_withNullValues() {
        Reservation r = new Reservation(null, null);
        assertNull(r.getFromDate());
        assertNull(r.getToDate());
    }

    @Test
    void testSetFromDate_setsCorrectly() {
        reservation.setFromDate("2024-03-15");
        assertEquals("2024-03-15", reservation.getFromDate());
    }

    @Test
    void testSetToDate_setsCorrectly() {
        reservation.setToDate("2024-03-20");
        assertEquals("2024-03-20", reservation.getToDate());
    }

    @Test
    void testSetFromDate_withNull() {
        reservation.setFromDate(null);
        assertNull(reservation.getFromDate());
    }

    @Test
    void testSetToDate_withNull() {
        reservation.setToDate(null);
        assertNull(reservation.getToDate());
    }

    @Test
    void testGetFromDate_returnsCorrectValue() {
        reservation.setFromDate("2024-06-01");
        assertEquals("2024-06-01", reservation.getFromDate());
    }

    @Test
    void testGetToDate_returnsCorrectValue() {
        reservation.setToDate("2024-06-15");
        assertEquals("2024-06-15", reservation.getToDate());
    }

    @Test
    void testSetAndGetBothDates() {
        reservation.setFromDate("2024-07-01");
        reservation.setToDate("2024-07-31");
        assertEquals("2024-07-01", reservation.getFromDate());
        assertEquals("2024-07-31", reservation.getToDate());
    }

    @Test
    void testOverwriteFromDate() {
        reservation.setFromDate("2024-01-01");
        reservation.setFromDate("2024-02-01");
        assertEquals("2024-02-01", reservation.getFromDate());
    }

    @Test
    void testOverwriteToDate() {
        reservation.setToDate("2024-01-31");
        reservation.setToDate("2024-02-28");
        assertEquals("2024-02-28", reservation.getToDate());
    }
}
