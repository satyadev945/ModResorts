package com.acme.modres.mbean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.acme.modres.mbean.reservation.ReservationList;
import com.google.gson.Gson;

public final class IOUtils {

  /**
   * Reads a classpath resource fully into memory. No data is written to the
   * local file system (ephemeral in cloud/container environments).
   */
  public static byte[] getBytesFromRelativePath(String path) {
    try (InputStream in = IOUtils.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        return null;
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
      return out.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private static Object parseClasspathJson(String path, Class<?> cls) {
    try (InputStream in = IOUtils.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        return null;
      }
      try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
        return new Gson().fromJson(reader, cls);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static OpMetadataList getOpListFromConfig() {
    try {
      return (OpMetadataList) parseClasspathJson("ops.json", OpMetadataList.class);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    try {
      return (ReservationList) parseClasspathJson("reservations.json", ReservationList.class);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

}
