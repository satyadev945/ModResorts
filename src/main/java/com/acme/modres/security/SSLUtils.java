package com.acme.modres.security;

import java.util.logging.Logger;

public class SSLUtils {
  private static final Logger logger = Logger.getLogger(SSLUtils.class.getName());

  /**
   * Returns an SSLContext configured with a permissive trust manager.
   * NOTE: This implementation is for development/demo purposes only.
   * In production, use a properly configured trust store with valid certificates.
   */
  public javax.net.ssl.SSLContext getContext() throws Exception {
    try {
      javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
      sc.init(null,
          new javax.net.ssl.TrustManager[]{ new FakeX509TrustManager() },
          new java.security.SecureRandom());
      return sc;
    } catch (Exception exc) {
      logger.severe("Failed to create SSLContext: " + exc.getMessage());
      throw new Exception("Failed to create SSLContext", exc);
    }
  }
}
