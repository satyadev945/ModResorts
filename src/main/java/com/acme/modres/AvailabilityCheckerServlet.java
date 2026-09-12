package com.acme.modres;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.acme.modres.mbean.IOUtils;
import com.acme.modres.mbean.reservation.DateChecker;
import com.acme.modres.mbean.reservation.ReservationCheckerData;
import com.acme.modres.mbean.reservation.Reservation;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());

  private static InitialContext context;

  private ReservationCheckerData reservationCheckerData;

  @Override
  public void init() {
    // load reserved dates
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

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      for (Reservation reservation : reservations) {
        try {
          LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), formatter);
          LocalDate toDate = LocalDate.parse(reservation.getToDate(), formatter);
          LocalDate selectedDate = reservationCheckerData.getSelectedDate();

          if (selectedDate.isAfter(fromDate) && selectedDate.isBefore(toDate)) {
            isAvailible = false;
            break;
          }
        } catch (DateTimeParseException ex) {
          ex.printStackTrace();
        }
      }

      reservationCheckerData.setAvailablility(isAvailible);

      // Adjust the status code based on availability
      if (!isAvailible) {
        statusCode = 201;
      }
    }

    // Send the response
    PrintWriter out = response.getWriter();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    out.print("{\"availability\": \"" + String.valueOf(reservationCheckerData.isAvailible()) + "\"}");
    response.setStatus(statusCode);
  }

  /**
   * Returns the weather information for a given city
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    doGet(request, response);
  }

  /**
   * Exports reservations by reading reservations.json from Amazon S3,
   * compressing it into a ZIP archive in memory, and uploading the result
   * back to Amazon S3. The S3 bucket name and object keys are resolved from
   * environment variables (S3_BUCKET_NAME, S3_RESERVATIONS_KEY,
   * S3_RESERVATIONS_ZIP_KEY) so that no absolute file-system paths are
   * required at runtime.
   *
   * @param selectedDateStr the selected date string (reserved for future use)
   * @return 0 on success, -1 on failure
   */
  protected int exportRevervations(String selectedDateStr) {
    // Resolve S3 configuration from environment variables – no hard-coded paths
    String bucketName   = System.getenv("S3_BUCKET_NAME");
    String sourceKey    = System.getenv().getOrDefault("S3_RESERVATIONS_KEY",     "reservations.json");
    String destKey      = System.getenv().getOrDefault("S3_RESERVATIONS_ZIP_KEY", "reservations.zip");

    try (S3Client s3 = S3Client.create()) {

      // --- Read reservations.json from S3 ---
      GetObjectRequest getRequest = GetObjectRequest.builder()
          .bucket(bucketName)
          .key(sourceKey)
          .build();

      ResponseBytes<GetObjectResponse> s3Object = s3.getObjectAsBytes(getRequest);
      byte[] sourceBytes = s3Object.asByteArray();

      // --- Build ZIP archive in memory ---
      // Use try-with-resources for ZipOutputStream to ensure it is always closed
      // and all ZIP data is flushed before reading the underlying byte array.
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
           ZipOutputStream zipOut = new ZipOutputStream(baos)) {
        ZipEntry zipEntry = new ZipEntry(sourceKey);
        zipOut.putNextEntry(zipEntry);

        byte[] buffer = new byte[1024];
        int offset = 0;
        while (offset < sourceBytes.length) {
          int length = Math.min(buffer.length, sourceBytes.length - offset);
          zipOut.write(sourceBytes, offset, length);
          offset += length;
        }
        zipOut.closeEntry();

        // Finish the ZIP stream before reading bytes
        zipOut.finish();
        byte[] zipBytes = baos.toByteArray();

        // --- Upload ZIP archive to S3 ---
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(destKey)
            .contentType("application/zip")
            .contentLength((long) zipBytes.length)
            .build();

        s3.putObject(putRequest, RequestBody.fromBytes(zipBytes));
      }

      return 0;

    } catch (S3Exception e) {
      logger.severe("S3 error during exportRevervations: " + e.awsErrorDetails().errorMessage());
      e.printStackTrace();
    } catch (IOException e) {
      logger.severe("IO error during exportRevervations: " + e.getMessage());
      e.printStackTrace();
    } catch (Throwable e) {
      logger.severe("Unexpected error during exportRevervations: " + e.getMessage());
      e.printStackTrace();
    }
    return -1;
  }

}
