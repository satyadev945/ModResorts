package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for ReservationList class.
 */
public class ReservationListTest {

    private ReservationList reservationList;

    @BeforeEach
    void setUp() {
        reservationList = new ReservationList();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        assertNotNull(reservationList);
    }

    @Test
    void testDefaultConstructor_emptyList() {
        assertNotNull(reservationList.getReservations());
        assertTrue(reservationList.getReservations().isEmpty());
    }

    @Test
    void testParameterizedConstructor_withList() {
        List<Reservation> list = new ArrayList<>();
        list.add(new Reservation("2024-01-01", "2024-01-10"));
        ReservationList rl = new ReservationList(list);
        assertNotNull(rl.getReservations());
        assertEquals(1, rl.getReservations().size());
    }

    @Test
    void testParameterizedConstructor_withEmptyList() {
        List<Reservation> list = new ArrayList<>();
        ReservationList rl = new ReservationList(list);
        assertNotNull(rl.getReservations());
        assertTrue(rl.getReservations().isEmpty());
    }

    @Test
    void testAdd_singleReservation() {
        Reservation r = new Reservation("2024-02-01", "2024-02-10");
        reservationList.add(r);
        assertEquals(1, reservationList.getReservations().size());
    }

    @Test
    void testAdd_multipleReservations() {
        reservationList.add(new Reservation("2024-01-01", "2024-01-10"));
        reservationList.add(new Reservation("2024-02-01", "2024-02-10"));
        reservationList.add(new Reservation("2024-03-01", "2024-03-10"));
        assertEquals(3, reservationList.getReservations().size());
    }

    @Test
    void testGetReservations_returnsCorrectList() {
        Reservation r1 = new Reservation("2024-01-01", "2024-01-10");
        Reservation r2 = new Reservation("2024-02-01", "2024-02-10");
        reservationList.add(r1);
        reservationList.add(r2);
        List<Reservation> result = reservationList.getReservations();
        assertEquals(2, result.size());
        assertEquals("2024-01-01", result.get(0).getFromDate());
        assertEquals("2024-02-01", result.get(1).getFromDate());
    }

    @Test
    void testAdd_nullReservation() {
        // Should not throw, just add null to list
        assertDoesNotThrow(() -> reservationList.add(null));
        assertEquals(1, reservationList.getReservations().size());
    }

    @Test
    void testParameterizedConstructor_withMultipleReservations() {
        List<Reservation> list = new ArrayList<>();
        list.add(new Reservation("2024-01-01", "2024-01-10"));
        list.add(new Reservation("2024-02-01", "2024-02-10"));
        ReservationList rl = new ReservationList(list);
        assertEquals(2, rl.getReservations().size());
    }
}
