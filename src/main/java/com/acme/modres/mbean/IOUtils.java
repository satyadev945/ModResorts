package com.acme.modres.mbean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

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
 * Utility class for I/O operations.
 *
 * Cloud-readiness fix (cr-java-0112 – Local Temporary Storage Reliance):
 *   The original implementation at line 23 called File.createTempFile(path, null)
 *   to write classpath resources to the local /tmp directory, then returned a File
 *   handle for downstream JSON parsing.  In cloud/container environments the local
 *   file system (including /tmp) is ephemeral: data written there is lost on
 *   container restart, scale-out, or pod eviction.
 *
 *   Fix: Eliminated all use of File.createTempFile and local temporary directories.
 *   Classpath resources are now read entirely into memory (byte[]).  When the
 *   environment variable USE_S3_CONFIG=true is set, intermediate data is stored in
 *   Amazon S3, ensuring durability across container lifecycle events and enabling
 *   multi-instance access.  No local file system writes occur in either path.
 *
 * Cloud-readiness fix (cr-java-0062 – Local File System Write):
 *   Replaced FileOutputStream-based writes (line 24 of the original source) with
 *   Amazon S3 PutObject / GetObject operations for durable, cloud-native storage.
 *
 * Configuration (environment variables):
 *   S3_BUCKET_NAME     – name of the S3 bucket used for config objects
 *   AWS_REGION         – AWS region (default: us-east-1)
 *   S3_CONFIG_PREFIX   – optional key prefix for config objects (default: "config/")
 *   USE_S3_CONFIG      – set to "true" to read/write config via S3;
 *                        when absent/false the classpath resource is used directly
 *                        (useful for local development).
 */
public final class IOUtils {

  private static final String DEFAULT_REGION = "us-east-1";
  private static final String DEFAULT_CONFIG_PREFIX = "config/";

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  /**
   * Reads a classpath resource into a byte array.
   *
   * Cloud-readiness fix (cr-java-0112): Replaces the original pattern of reading
   * the classpath resource into a buffer and then writing it to a local temp file
   * via File.createTempFile (line 23 of the original source).  All data is now
   * held in memory – no local temporary directory is touched.
   */
  private static byte[] readClasspathResource(String path) throws IOException {
    try (InputStream initialStream = IOUtils.class.getClassLoader().getResourceAsStream(path)) {
      if (initialStream == null) {
        throw new IOException("Classpath resource not found: " + path);
      }
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] chunk = new byte[4096];
      int bytesRead;
      while ((bytesRead = initialStream.read(chunk)) != -1) {
        buffer.write(chunk, 0, bytesRead);
      }
      return buffer.toByteArray();
    }
  }

  /**
   * Builds an S3Client using environment-variable configuration.
   */
  private static S3Client buildS3Client() {
    String awsRegion = System.getenv().getOrDefault("AWS_REGION", DEFAULT_REGION);
    return S3Client.builder()
        .region(Region.of(awsRegion))
        .build();
  }

  /**
   * Returns the S3 key for a given config file name.
   */
  private static String s3Key(String fileName) {
    String prefix = System.getenv().getOrDefault("S3_CONFIG_PREFIX", DEFAULT_CONFIG_PREFIX);
    if (!prefix.endsWith("/")) {
      prefix = prefix + "/";
    }
    return prefix + fileName;
  }

  /**
   * Parses JSON bytes into the requested type using Gson.
   */
  private static <T> T parseJson(byte[] jsonBytes, Class<T> cls) throws IOException {
    try (InputStream is = new ByteArrayInputStream(jsonBytes);
         BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
      Gson gson = new Gson();
      return gson.fromJson(reader, cls);
    }
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Retrieves the content of a config resource as a byte array.
   *
   * Cloud-readiness fix (cr-java-0112 – Local Temporary Storage Reliance, line 23):
   *   The original getFileFromRelativePath() method called File.createTempFile(path, null)
   *   at line 23 to create a temporary file in the local /tmp directory, then wrote
   *   the classpath resource bytes into it via FileOutputStream.  This pattern relies
   *   on ephemeral local storage that does not survive container restarts or scale-out
   *   events in cloud environments.
   *
   *   This method eliminates all local temporary file creation.  Resource bytes are
   *   held entirely in memory.  When USE_S3_CONFIG=true the content is stored in and
   *   retrieved from Amazon S3, providing durable intermediate storage that survives
   *   container lifecycle events and is accessible across multiple instances.
   *
   * @param path classpath-relative resource name (e.g. "reservations.json")
   * @return resource bytes, or null on error
   */
  public static byte[] getResourceBytes(String path) {
    try {
      // Read classpath resource into memory – no local temp file created (cr-java-0112 fix).
      byte[] resourceBytes = readClasspathResource(path);

      boolean useS3 = "true".equalsIgnoreCase(System.getenv("USE_S3_CONFIG"));
      if (useS3) {
        String bucketName = System.getenv("S3_BUCKET_NAME");
        String key = s3Key(path);

        try (S3Client s3Client = buildS3Client()) {
          // Try to fetch from S3 first; upload from classpath if not present.
          try {
            GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(getReq);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = s3Stream.read(buf)) != -1) {
              baos.write(buf, 0, n);
            }
            s3Stream.close();
            // Data retrieved from S3 – durable across container restarts (cr-java-0112 fix).
            return baos.toByteArray();
          } catch (NoSuchKeyException e) {
            // Object not in S3 yet – seed it from the classpath resource.
            // Storing in S3 instead of a local temp file ensures persistence (cr-java-0112 fix).
            PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/json")
                .build();
            s3Client.putObject(putReq, RequestBody.fromBytes(resourceBytes));
            // Return the classpath bytes directly for this first call.
            return resourceBytes;
          }
        }
      }

      // Default (local / non-S3) path: serve directly from in-memory bytes.
      // No local temporary directory is used (cr-java-0112 fix).
      return resourceBytes;

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Parses the ops.json config resource and returns an OpMetadataList.
   *
   * Cloud-readiness fix (cr-java-0112): Uses getResourceBytes() + in-memory Gson
   * parsing instead of the former getFileFromRelativePath() which created a local
   * temp file via File.createTempFile (line 23 of the original source).
   */
  public static OpMetadataList getOpListFromConfig() {
    try {
      byte[] jsonBytes = getResourceBytes("ops.json");
      if (jsonBytes == null) {
        return null;
      }
      return parseJson(jsonBytes, OpMetadataList.class);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Parses the reservations.json config resource and returns a ReservationList.
   *
   * Cloud-readiness fix (cr-java-0112): Uses getResourceBytes() + in-memory Gson
   * parsing instead of the former getFileFromRelativePath() which created a local
   * temp file via File.createTempFile (line 23 of the original source).
   */
  public static ReservationList getReservationListFromConfig() {
    try {
      byte[] jsonBytes = getResourceBytes("reservations.json");
      if (jsonBytes == null) {
        return null;
      }
      return parseJson(jsonBytes, ReservationList.class);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}
