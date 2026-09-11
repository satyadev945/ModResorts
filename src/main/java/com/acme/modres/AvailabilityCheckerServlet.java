package com.acme.modres;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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

      // cr-java-0111: Replaced java.util.Date / SimpleDateFormat with java.time API.
      // DateTimeFormatter + LocalDate are immutable, thread-safe, and timezone-neutral
      // (date-only comparisons use no timezone offset), eliminating clock/timezone
      // inconsistencies across distributed cloud nodes.
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      for (Reservation reservation : reservations) {
        try {
          LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), formatter);
          LocalDate toDate   = LocalDate.parse(reservation.getToDate(),   formatter);
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
   * compressing it into a zip archive in memory, and uploading the result
   * back to S3.
   *
   * Cloud-readiness fix (cr-java-0062 – Local File System Write Operations):
   *   All local file write operations have been replaced with Amazon S3 operations
   *   to ensure data durability in containerised and serverless environments.
   *   - Source  : reads reservations.json from S3 bucket (key: "reservations.json")
   *   - Process : compresses content into a zip archive entirely in memory
   *   - Sink    : uploads the zip archive to S3 (key: "reservations.zip")
   *
   * Cloud-readiness fix (cr-java-0098 – Resource Leaks):
   *   All AutoCloseable resources (S3Client, ResponseInputStream, ZipOutputStream,
   *   ZipInputStream) are now managed via try-with-resources to guarantee automatic
   *   closure and prevent resource exhaustion in containerised AWS environments.
   *
   * Configuration (environment variables – 12-factor app):
   *   S3_BUCKET_NAME : name of the S3 bucket that holds reservation data
   *   AWS_REGION     : AWS region (defaults to "us-east-1" if not set)
   */
  protected int exportRevervations(String selectedDateStr) {
    // Resolve S3 configuration from environment variables (12-factor app principle)
    String bucketName = System.getenv("S3_BUCKET_NAME");
    String awsRegion  = System.getenv("AWS_REGION");
    if (awsRegion == null || awsRegion.isEmpty()) {
      awsRegion = "us-east-1";
    }

    String sourceKey      = "reservations.json";
    String destinationKey = "reservations.zip";

    // cr-java-0098: S3Client wrapped in try-with-resources for automatic closure
    try (S3Client s3Client = S3Client.builder()
        .region(Region.of(awsRegion))
        .build()) {

      // --- Read reservations.json from S3 (replaces FileInputStream on local file) ---
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucketName)
          .key(sourceKey)
          .build();

      // cr-java-0098: ResponseInputStream and ZipOutputStream wrapped in
      // try-with-resources to ensure they are always closed, even on exception
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ResponseInputStream<GetObjectResponse> s3ObjectStream = s3Client.getObject(getObjectRequest);
           ZipOutputStream zipOut = new ZipOutputStream(baos)) {

        // --- Compress the content into a zip in memory ---
        ZipEntry zipEntry = new ZipEntry(sourceKey);
        zipOut.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = s3ObjectStream.read(bytes)) >= 0) {
          zipOut.write(bytes, 0, length);
        }
        zipOut.closeEntry();
      }

      byte[] zipBytes = baos.toByteArray();

      // --- Validate the in-memory zip before uploading ---
      // cr-java-0098: ZipInputStream already uses try-with-resources (preserved)
      try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
        if (zis.getNextEntry() == null) {
          logger.warning("Generated zip archive appears to be empty or invalid.");
          return -1;
        }
      }

      // --- Upload the zip archive to S3 (replaces local FileOutputStream to user.home) ---
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucketName)
          .key(destinationKey)
          .contentType("application/zip")
          .build();

      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(zipBytes));

      return 0;

    } catch (IOException e) {
      e.printStackTrace();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return -1;
  }

}
