package com.acme.modres.mbean;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.acme.modres.mbean.reservation.ReservationList;
import com.google.gson.Gson;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Utility class for reading configuration resources.
 *
 * Cloud-readiness fix (cr-java-0112): Replaced local temporary file storage with
 * Amazon S3 for persistent intermediate data. Previously, getFileFromRelativePath()
 * wrote classpath resources to a local temp file via File.createTempFile() /
 * FileOutputStream, which is ephemeral in containerised/cloud environments and
 * data is lost during container lifecycle events.
 *
 * The fix eliminates reliance on ephemeral local temporary directories by:
 *  1. Reading classpath resources in-memory using Gson (primary path).
 *  2. Providing an S3-backed store/retrieve mechanism for intermediate data
 *     that must survive container restarts and be accessible across multiple
 *     application instances.
 *
 * S3 bucket name is read from the environment variable S3_TEMP_BUCKET.
 * S3 region is read from the environment variable AWS_REGION (defaults to us-east-1).
 */
public final class IOUtils {

  /** Environment variable that holds the S3 bucket name for intermediate/temp data. */
  private static final String S3_BUCKET_ENV = "S3_TEMP_BUCKET";

  /** Environment variable for the AWS region (standard AWS SDK variable). */
  private static final String AWS_REGION_ENV = "AWS_REGION";

  /** Default AWS region when AWS_REGION is not set. */
  private static final String DEFAULT_REGION = "us-east-1";

  // -------------------------------------------------------------------------
  // S3 helper methods
  // -------------------------------------------------------------------------

  /**
   * Returns a lazily-created S3Client configured from environment variables.
   * The region is read from {@code AWS_REGION}; defaults to {@code us-east-1}.
   */
  private static S3Client buildS3Client() {
    String regionStr = System.getenv(AWS_REGION_ENV);
    Region region = (regionStr != null && !regionStr.isEmpty())
        ? Region.of(regionStr)
        : Region.of(DEFAULT_REGION);
    return S3Client.builder().region(region).build();
  }

  /**
   * Stores the given content string in Amazon S3 under the specified key.
   * The bucket name is read from the {@code S3_TEMP_BUCKET} environment variable.
   *
   * @param key     the S3 object key (e.g. "temp/ops.json")
   * @param content the UTF-8 string content to store
   */
  public static void storeIntermediateDataToS3(String key, String content) {
    String bucket = System.getenv(S3_BUCKET_ENV);
    if (bucket == null || bucket.isEmpty()) {
      System.err.println("IOUtils: S3_TEMP_BUCKET environment variable is not set; "
          + "cannot store intermediate data to S3 for key: " + key);
      return;
    }
    try (S3Client s3 = buildS3Client()) {
      byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
      PutObjectRequest putRequest = PutObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .contentType("application/json")
          .contentLength((long) bytes.length)
          .build();
      s3.putObject(putRequest, RequestBody.fromBytes(bytes));
      System.out.println("IOUtils: stored intermediate data to S3 s3://" + bucket + "/" + key);
    } catch (Exception e) {
      System.err.println("IOUtils: failed to store intermediate data to S3 for key: " + key);
      e.printStackTrace();
    }
  }

  /**
   * Retrieves intermediate data from Amazon S3 and parses it as the given type.
   * Falls back to reading from the classpath if the S3 object does not exist or
   * the bucket is not configured.
   *
   * @param s3Key           the S3 object key (e.g. "temp/ops.json")
   * @param classpathFallback the classpath resource path used as fallback
   * @param cls             the target class to deserialise into
   * @return the deserialised object, or {@code null} if neither source is available
   */
  public static <T> T retrieveIntermediateDataFromS3(String s3Key, String classpathFallback, Class<T> cls) {
    String bucket = System.getenv(S3_BUCKET_ENV);
    if (bucket != null && !bucket.isEmpty()) {
      try (S3Client s3 = buildS3Client()) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(s3Key)
            .build();
        try (ResponseInputStream<GetObjectResponse> s3Object = s3.getObject(getRequest)) {
          Gson gson = new Gson();
          BufferedReader reader = new BufferedReader(
              new InputStreamReader(s3Object, StandardCharsets.UTF_8));
          T result = gson.fromJson(reader, cls);
          System.out.println("IOUtils: retrieved intermediate data from S3 s3://" + bucket + "/" + s3Key);
          return result;
        }
      } catch (NoSuchKeyException e) {
        System.out.println("IOUtils: S3 key not found (" + s3Key + "), falling back to classpath resource.");
      } catch (Exception e) {
        System.err.println("IOUtils: error reading from S3 for key: " + s3Key + "; falling back to classpath.");
        e.printStackTrace();
      }
    }
    // Fallback: read directly from classpath (no local temp file created)
    return parseResourceAsJson(classpathFallback, cls);
  }

  // -------------------------------------------------------------------------
  // In-memory classpath helpers (no local file system writes)
  // -------------------------------------------------------------------------

  /**
   * Reads a classpath resource and parses it as the given type using Gson.
   * No local file is created; the resource is consumed entirely in memory.
   *
   * @param path the classpath-relative resource path (e.g. "ops.json")
   * @param cls  the target class to deserialise into
   * @return the deserialised object, or {@code null} if the resource cannot be read
   */
  public static <T> T parseResourceAsJson(String path, Class<T> cls) {
    InputStream initialStream = null;
    try {
      initialStream = IOUtils.class.getClassLoader().getResourceAsStream(path);
      if (initialStream == null) {
        System.err.println("IOUtils: classpath resource not found: " + path);
        return null;
      }
      Gson gson = new Gson();
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(initialStream, StandardCharsets.UTF_8));
      return gson.fromJson(reader, cls);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      if (initialStream != null) {
        try {
          initialStream.close();
        } catch (IOException e) {
          // ignore close failure
        }
      }
    }
  }

  // -------------------------------------------------------------------------
  // Public API – uses S3 for persistent intermediate data (cr-java-0112)
  // -------------------------------------------------------------------------

  /**
   * Returns the operations metadata list.
   *
   * Attempts to retrieve the data from Amazon S3 (persistent intermediate store)
   * first; falls back to reading ops.json directly from the classpath if S3 is
   * unavailable or the object does not yet exist.
   *
   * No local temporary file is created at any point.
   */
  public static OpMetadataList getOpListFromConfig() {
    // cr-java-0112: use S3 for persistent intermediate data instead of local /tmp
    return retrieveIntermediateDataFromS3("temp/ops.json", "ops.json", OpMetadataList.class);
  }

  /**
   * Returns the reservation list.
   *
   * Attempts to retrieve the data from Amazon S3 (persistent intermediate store)
   * first; falls back to reading reservations.json directly from the classpath if
   * S3 is unavailable or the object does not yet exist.
   *
   * No local temporary file is created at any point.
   */
  public static ReservationList getReservationListFromConfig() {
    // cr-java-0112: use S3 for persistent intermediate data instead of local /tmp
    return retrieveIntermediateDataFromS3("temp/reservations.json", "reservations.json", ReservationList.class);
  }

}
