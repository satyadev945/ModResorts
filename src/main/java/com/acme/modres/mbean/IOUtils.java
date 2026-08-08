package com.acme.modres.mbean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

public final class IOUtils {
  private static final Logger LOGGER = Logger.getLogger(IOUtils.class.getName());
  private static final String BLOB_ENDPOINT_ENV = "AZURE_STORAGE_BLOB_ENDPOINT";
  private static final String BLOB_CONTAINER_ENV = "AZURE_STORAGE_CONTAINER_NAME";
  private static final String SERVICE_BUS_NAMESPACE_ENV = "AZURE_SERVICEBUS_NAMESPACE";

  private IOUtils() {
  }

  public static byte[] readResourceBytes(String path) {
    try (InputStream initialStream = IOUtils.class.getClassLoader().getResourceAsStream(path);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      if (initialStream == null) {
        throw new IOException("Resource not found: " + path);
      }
      byte[] data = new byte[4096];
      int nRead;
      while ((nRead = initialStream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      return buffer.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read resource bytes for " + path, e);
    }
  }

  public static InputStream getResourceStream(String path) {
    return new ByteArrayInputStream(readResourceBytes(path));
  }

  public static void uploadToBlobStorage(String blobName, InputStream data, long length) {
    BlobClient blobClient = getBlobContainerClient().getBlobClient(blobName);
    blobClient.upload(data, length, true);
  }

  public static void scheduleServiceBusMessage(String queueName, String payload, OffsetDateTime scheduledTime) {
    String namespace = System.getenv(SERVICE_BUS_NAMESPACE_ENV);
    if (namespace == null || namespace.trim().isEmpty()) {
      throw new IllegalStateException("Missing Azure Service Bus namespace configuration");
    }

    TokenCredential credential = new DefaultAzureCredentialBuilder().build();
    try (ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
        .credential(namespace, credential)
        .sender()
        .queueName(queueName)
        .buildClient()) {
      ServiceBusMessage message = new ServiceBusMessage(payload);
      message.setScheduledEnqueueTime(scheduledTime);
      senderClient.scheduleMessage(message, scheduledTime);
    }
  }

  private static BlobContainerClient getBlobContainerClient() {
    String endpoint = System.getenv(BLOB_ENDPOINT_ENV);
    String containerName = System.getenv(BLOB_CONTAINER_ENV);
    if (endpoint == null || endpoint.trim().isEmpty() || containerName == null || containerName.trim().isEmpty()) {
      throw new IllegalStateException("Azure Blob Storage endpoint or container name is not configured");
    }

    TokenCredential credential = new DefaultAzureCredentialBuilder().build();
    BlobContainerClient containerClient = new BlobContainerClientBuilder()
        .endpoint(endpoint)
        .credential(credential)
        .containerName(containerName)
        .buildClient();
    if (!containerClient.exists()) {
      throw new IllegalStateException("Configured Azure Blob container does not exist: " + containerName);
    }
    return containerClient;
  }

  public static OpMetadataList getOpListFromConfig() {
    try (JsonInputStream is = new JsonInputStream(getResourceStream("ops.json"))) {
      return (OpMetadataList) is.parseJsonAs(OpMetadataList.class);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Unable to load ops configuration", e);
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    try (JsonInputStream is = new JsonInputStream(getResourceStream("reservations.json"))) {
      return (ReservationList) is.parseJsonAs(ReservationList.class);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Unable to load reservation configuration", e);
      return null;
    }
  }
}
