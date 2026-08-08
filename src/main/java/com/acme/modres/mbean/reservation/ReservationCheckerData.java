package com.acme.modres.mbean.reservation;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import com.acme.modres.Constants;
import com.acme.modres.mbean.IOUtils;

public class ReservationCheckerData {
  private ReservationList reservations;
  private Date selectedDate;
  private boolean available;

  public ReservationCheckerData(ReservationList reservations) {
    this.reservations = reservations;
    this.available = true;
  }

  public ReservationList getReservationList() {
    return reservations;
  }

  public Date getSelectedDate() {
    return selectedDate;
  }

  public boolean setSelectedDate(String dateStr) {
    try {
      selectedDate = new SimpleDateFormat(Constants.DATA_FORMAT).parse(dateStr);
      String queueName = System.getenv("AZURE_SERVICEBUS_QUEUE_NAME");
      if (queueName != null && !queueName.trim().isEmpty()) {
        IOUtils.scheduleServiceBusMessage(queueName, "reservation-date-selected:" + dateStr,
            OffsetDateTime.now(ZoneOffset.UTC));
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  public boolean isAvailible() {
    return available;
  }

  public void setAvailablility(boolean available) {
    this.available = available;
  }
}
