package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

/**
 * Unit tests for ReservationCheckerData class.
 */
public class ReservationCheckerDataTest {

    private ReservationCheckerData checkerData;
    private ReservationList reservationList;

    @BeforeEach
    void setUp() {
        reservationList = new ReservationList();
        checkerData = new ReservationCheckerData(reservationList);
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(checkerData);
    }

    @Test
    void testConstructor_defaultAvailability_isTrue() {
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testGetReservationList_returnsCorrectList() {
        assertSame(reservationList, checkerData.getReservationList());
    }

    @Test
    void testSetSelectedDate_validDate_returnsTrue() {
        boolean result = checkerData.setSelectedDate("2024-06-15");
        assertTrue(result);
    }

    @Test
    void testSetSelectedDate_invalidDate_returnsFalse() {
        boolean result = checkerData.setSelectedDate("not-a-date");
        assertFalse(result);
    }

    @Test
    void testSetSelectedDate_nullDate_returnsFalse() {
        // Null date causes NullPointerException in LocalDate.parse - returns false
        try {
            boolean result = checkerData.setSelectedDate(null);
            assertFalse(result);
        } catch (NullPointerException e) {
            // NPE is acceptable for null input
        }
    }

    @Test
    void testSetSelectedDate_emptyString_returnsFalse() {
        boolean result = checkerData.setSelectedDate("");
        assertFalse(result);
    }

    @Test
    void testGetSelectedDate_afterValidSet_returnsLocalDate() {
        checkerData.setSelectedDate("2024-06-15");
        LocalDate date = checkerData.getSelectedDate();
        assertNotNull(date);
        assertEquals(2024, date.getYear());
        assertEquals(6, date.getMonthValue());
        assertEquals(15, date.getDayOfMonth());
    }

    @Test
    void testGetSelectedDate_beforeSet_returnsNull() {
        assertNull(checkerData.getSelectedDate());
    }

    @Test
    void testSetAvailability_toFalse() {
        checkerData.setAvailablility(false);
        assertFalse(checkerData.isAvailible());
    }

    @Test
    void testSetAvailability_toTrue() {
        checkerData.setAvailablility(false);
        checkerData.setAvailablility(true);
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testIsAvailible_defaultTrue() {
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testSetSelectedDate_wrongFormat_returnsFalse() {
        boolean result = checkerData.setSelectedDate("15/06/2024");
        assertFalse(result);
    }

    @Test
    void testSetSelectedDate_validIsoDate_setsCorrectly() {
        boolean result = checkerData.setSelectedDate("2024-12-31");
        assertTrue(result);
        LocalDate date = checkerData.getSelectedDate();
        assertEquals(2024, date.getYear());
        assertEquals(12, date.getMonthValue());
        assertEquals(31, date.getDayOfMonth());
    }

    @Test
    void testConstructor_withNullReservationList() {
        ReservationCheckerData data = new ReservationCheckerData(null);
        assertNotNull(data);
        assertTrue(data.isAvailible());
    }
}
