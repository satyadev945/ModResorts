package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReservationListTest {

    @Test
    void testDefaultConstructor() {
        ReservationList list = new ReservationList();
        assertNotNull(list.getReservations());
        assertEquals(0, list.getReservations().size());
    }

    @Test
    void testParameterizedConstructor() {
        List<Reservation> resList = new ArrayList<>();
        resList.add(new Reservation("2023-01-01", "2023-01-10"));
        ReservationList list = new ReservationList(resList);
        assertEquals(1, list.getReservations().size());
    }

    @Test
    void testAddReservation() {
        ReservationList list = new ReservationList();
        Reservation res = new Reservation("2023-01-01", "2023-01-10");
        list.add(res);
        assertEquals(1, list.getReservations().size());
        assertEquals(res, list.getReservations().get(0));
    }
}
