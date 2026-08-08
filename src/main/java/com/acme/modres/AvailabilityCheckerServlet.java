package com.acme.modres;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.acme.modres.mbean.IOUtils;
import com.acme.modres.mbean.reservation.Reservation;
import com.acme.modres.mbean.reservation.ReservationCheckerData;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());
  private static final String EXPORT_QUEUE_NAME_ENV = "AZURE_SERVICEBUS_QUEUE_NAME";

  private ReservationCheckerData reservationCheckerData;

  @Override
  public void init() {
    this.reservationCheckerData = new ReservationCheckerData(IOUtils.getReservationListFromConfig());
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    String methodName = "doGet";
    logger.entering(AvailabilityCheckerServlet.class.getName(), methodName);
    int statusCode = 200;

    String selectedDateStr = request.getParameter("date");
    boolean parsedDate = reservationCheckerData.setSelectedDate(selectedDateStr);
    if (!parsedDate || reservationCheckerData.getReservationList() == null) {
      statusCode = 500;
      reservationCheckerData.setAvailablility(false);
    } else {
      List<Reservation> reservations = reservationCheckerData.getReservationList().getReservations();
      boolean isAvailible = true;

      for (Reservation reservation : reservations) {
        try {
          Date fromDate = new SimpleDateFormat(Constants.DATA_FORMAT).parse(reservation.getFromDate());
          Date toDate = new SimpleDateFormat(Constants.DATA_FORMAT).parse(reservation.getToDate());
          Date selectedDate = reservationCheckerData.getSelectedDate();

          if (selectedDate.after(fromDate) && selectedDate.before(toDate)) {
            isAvailible = false;
            break;
          }
        } catch (ParseException ex) {
          logger.log(Level.WARNING, "Unable to parse reservation dates", ex);
        }
      }

      reservationCheckerData.setAvailablility(isAvailible);
      if (!isAvailible) {
        statusCode = 201;
      }
    }

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    try (PrintWriter out = response.getWriter()) {
      out.print("{\"availability\": \"" + String.valueOf(reservationCheckerData.isAvailible()) + "\"}");
    }
    response.setStatus(statusCode);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doGet(request, response);
  }

  protected int exportRevervations(String selectedDateStr) {
    try {
      byte[] reservationBytes = IOUtils.readResourceBytes("reservations.json");
      byte[] zipBytes = createZipArchive("reservations.json", reservationBytes);
      String blobName = "exports/reservations-" + sanitizeDate(selectedDateStr) + ".zip";
      IOUtils.uploadToBlobStorage(blobName, new ByteArrayInputStream(zipBytes), zipBytes.length);

      String queueName = System.getenv(EXPORT_QUEUE_NAME_ENV);
      if (queueName != null && !queueName.trim().isEmpty()) {
        IOUtils.scheduleServiceBusMessage(queueName,
            "reservation-export:" + blobName,
            OffsetDateTime.now(java.time.ZoneOffset.UTC));
      }
      return 0;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to export reservations to Azure services", e);
      return -1;
    }
  }

  private byte[] createZipArchive(String entryName, byte[] content) throws IOException {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream)) {
      ZipEntry zipEntry = new ZipEntry(entryName);
      zipOut.putNextEntry(zipEntry);
      zipOut.write(content);
      zipOut.closeEntry();
      zipOut.finish();
      return byteArrayOutputStream.toByteArray();
    }
  }

  private String sanitizeDate(String selectedDateStr) {
    if (selectedDateStr == null || selectedDateStr.trim().isEmpty()) {
      return "latest";
    }
    return selectedDateStr.replaceAll("[^a-zA-Z0-9_-]", "-");
  }
}
