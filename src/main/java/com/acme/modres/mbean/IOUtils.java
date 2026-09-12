package com.acme.modres.mbean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import com.acme.modres.mbean.reservation.ReservationList;
import com.google.gson.Gson;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Utility class for reading application configuration data.
 *
 * <p>All read operations are performed against Amazon S3 to ensure data
 * durability and availability in containerised / cloud environments.  Local
 * file-system writes have been removed so that no ephemeral state is created
 * on the container's file system (fixes cr-java-0062).
 *
 * <p>S3 configuration is resolved from environment variables:
 * <ul>
 *   <li>{@code S3_BUCKET_NAME}   – name of the S3 bucket that holds config files</li>
 *   <li>{@code S3_OPS_KEY}       – object key for ops.json        (default: "ops.json")</li>
 *   <li>{@code S3_RESERVATIONS_KEY} – object key for reservations.json (default: "reservations.json")</li>
 * </ul>
 *
 * <p>If the S3 bucket name is not configured the implementation falls back to
 * reading the resource from the application classpath so that local / test
 * environments continue to work without AWS credentials.
 */
public final class IOUtils {

    private static final Logger logger = Logger.getLogger(IOUtils.class.getName());

    private IOUtils() {
        // utility class – no instances
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Reads the content of an S3 object and returns it as a UTF-8 string.
     * Returns {@code null} when the object cannot be retrieved.
     */
    private static String readStringFromS3(S3Client s3, String bucketName, String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            ResponseBytes<GetObjectResponse> response = s3.getObjectAsBytes(request);
            return response.asUtf8String();
        } catch (S3Exception e) {
            logger.warning("Could not read s3://" + bucketName + "/" + key
                    + " – " + e.awsErrorDetails().errorMessage());
            return null;
        }
    }

    /**
     * Reads the content of a classpath resource and returns it as a UTF-8 string.
     * Returns {@code null} when the resource is not found.
     */
    private static String readStringFromClasspath(String resourceName) {
        try (InputStream is = IOUtils.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                logger.warning("Classpath resource not found: " + resourceName);
                return null;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException e) {
            logger.warning("Failed to read classpath resource " + resourceName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Resolves JSON content for the given resource name.
     *
     * <p>Strategy:
     * <ol>
     *   <li>If {@code S3_BUCKET_NAME} is set, attempt to read from S3.</li>
     *   <li>Fall back to the application classpath (for local / test environments).</li>
     * </ol>
     *
     * <p>No data is ever written to the local file system.
     */
    private static String resolveJsonContent(String resourceName, String s3KeyEnvVar) {
        String bucketName = System.getenv("S3_BUCKET_NAME");

        if (bucketName != null && !bucketName.isEmpty()) {
            String key = System.getenv().getOrDefault(s3KeyEnvVar, resourceName);
            try (S3Client s3 = S3Client.create()) {
                String content = readStringFromS3(s3, bucketName, key);
                if (content != null) {
                    logger.info("Loaded " + resourceName + " from S3 (s3://" + bucketName + "/" + key + ")");
                    return content;
                }
            } catch (Exception e) {
                logger.warning("S3Client creation failed, falling back to classpath: " + e.getMessage());
            }
        }

        // Fallback: read from classpath (no local file write)
        logger.info("Loading " + resourceName + " from classpath (S3_BUCKET_NAME not configured or S3 read failed)");
        return readStringFromClasspath(resourceName);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Loads the operations metadata list.
     *
     * <p>Reads {@code ops.json} from S3 (bucket = {@code S3_BUCKET_NAME},
     * key = {@code S3_OPS_KEY}) or falls back to the classpath resource.
     * No local file-system write is performed.
     *
     * @return the parsed {@link OpMetadataList}, or {@code null} on error
     */
    public static OpMetadataList getOpListFromConfig() {
        String json = resolveJsonContent("ops.json", "S3_OPS_KEY");
        if (json == null) {
            return null;
        }
        try {
            Gson gson = new Gson();
            return gson.fromJson(json, OpMetadataList.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads the reservation list.
     *
     * <p>Reads {@code reservations.json} from S3 (bucket = {@code S3_BUCKET_NAME},
     * key = {@code S3_RESERVATIONS_KEY}) or falls back to the classpath resource.
     * No local file-system write is performed.
     *
     * @return the parsed {@link ReservationList}, or {@code null} on error
     */
    public static ReservationList getReservationListFromConfig() {
        String json = resolveJsonContent("reservations.json", "S3_RESERVATIONS_KEY");
        if (json == null) {
            return null;
        }
        try {
            Gson gson = new Gson();
            return gson.fromJson(json, ReservationList.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
