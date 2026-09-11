package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.acme.modres.Constants;

/**
 * Holds the state for a reservation availability check.
 *
 * cr-java-0111: Replaced java.util.Date / SimpleDateFormat with java.time API.
 * The selectedDate field is now a java.time.LocalDate (immutable, timezone-neutral)
 * and parsing uses DateTimeFormatter, eliminating server-local timezone dependencies
 * that cause scheduling failures in distributed cloud environments.
 */
public class ReservationCheckerData {
  private ReservationList reservations;
  // cr-java-0111: was java.util.Date – replaced with java.time.LocalDate (line 28)
  private LocalDate selectedDate;
  private boolean available; // changed from Boolean to boolean

  public ReservationCheckerData(ReservationList reservations) {
    this.reservations = reservations;
    this.available = true;
  }

  public ReservationList getReservationList() {
    return reservations;
  }

  public LocalDate getSelectedDate() {
    return selectedDate;
  }

  public boolean setSelectedDate(String dateStr) {
    try {
      // cr-java-0111: DateTimeFormatter replaces SimpleDateFormat; LocalDate is
      // immutable and thread-safe, with no timezone offset for date-only values.
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      selectedDate = LocalDate.parse(dateStr, formatter);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  public boolean isAvailible() {
    return available;
  }

  public void setAvailablility(boolean available) { // fix parameter type
    this.available = available;
  }
}
