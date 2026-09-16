package com.acme.modres.security;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * A permissive X509TrustManager that trusts all certificates.
 * NOTE: This implementation is for development/demo purposes only.
 * In production, use a properly configured trust store with valid certificates.
 */
public class FakeX509TrustManager implements X509TrustManager {

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) {
    // Intentionally permissive for demo purposes - do not use in production
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) {
    // Intentionally permissive for demo purposes - do not use in production
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[0];
  }
}
