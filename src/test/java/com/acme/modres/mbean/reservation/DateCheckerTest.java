package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DateCheckerTest {

    @Test
    void testRun_Available() {
        ReservationList resList = new ReservationList();
        ReservationCheckerData data = new ReservationCheckerData(resList);
        data.setSelectedDate("01/01/2023");
        
        DateChecker checker = new DateChecker(data);
        checker.run();
        
        assertTrue(data.isAvailible());
    }

    @Test
    void testRun_NotAvailable() {
        ReservationList resList = new ReservationList();
        resList.add(new Reservation("01/01/2023", "01/10/2023"));
        ReservationCheckerData data = new ReservationCheckerData(resList);
        data.setSelectedDate("01/05/2023");
        
        DateChecker checker = new DateChecker(data);
        checker.run();
        
        assertFalse(data.isAvailible());
    }

    @Test
    void testRun_BoundaryDate() {
        ReservationList resList = new ReservationList();
        resList.add(new Reservation("01/01/2023", "01/10/2023"));
        ReservationCheckerData data = new ReservationCheckerData(resList);
        
        // Exactly on fromDate - should be available (since it uses .after())
        data.setSelectedDate("01/01/2023");
        DateChecker checker = new DateChecker(data);
        checker.run();
        assertTrue(data.isAvailible());
        
        // Exactly on toDate - should be available (since it uses .before())
        data.setSelectedDate("01/10/2023");
        checker.run();
        assertTrue(data.isAvailible());
    }

    @Test
    void testRun_InvalidDateInReservation() {
        ReservationList resList = new ReservationList();
        resList.add(new Reservation("invalid", "invalid"));
        ReservationCheckerData data = new ReservationCheckerData(resList);
        data.setSelectedDate("01/01/2023");
        
        DateChecker checker = new DateChecker(data);
        checker.run();
        
        // Should still be available if parsing fails
        assertTrue(data.isAvailible());
    }
}
