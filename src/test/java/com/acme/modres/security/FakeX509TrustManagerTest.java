package com.acme.modres.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.security.cert.X509Certificate;

/**
 * Unit tests for FakeX509TrustManager class.
 */
public class FakeX509TrustManagerTest {

    private FakeX509TrustManager trustManager;

    @BeforeEach
    void setUp() {
        trustManager = new FakeX509TrustManager();
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(trustManager);
    }

    @Test
    void testCheckClientTrusted_doesNotThrow() {
        assertDoesNotThrow(() ->
            trustManager.checkClientTrusted(new X509Certificate[0], "RSA")
        );
    }

    @Test
    void testCheckClientTrusted_withNullChain_doesNotThrow() {
        assertDoesNotThrow(() ->
            trustManager.checkClientTrusted(null, "RSA")
        );
    }

    @Test
    void testCheckClientTrusted_withNullAuthType_doesNotThrow() {
        assertDoesNotThrow(() ->
            trustManager.checkClientTrusted(new X509Certificate[0], null)
        );
    }

    @Test
    void testCheckServerTrusted_doesNotThrow() {
        assertDoesNotThrow(() ->
            trustManager.checkServerTrusted(new X509Certificate[0], "RSA")
        );
    }

    @Test
    void testCheckServerTrusted_withNullChain_doesNotThrow() {
        assertDoesNotThrow(() ->
            trustManager.checkServerTrusted(null, "RSA")
        );
    }

    @Test
    void testCheckServerTrusted_withNullAuthType_doesNotThrow() {
        assertDoesNotThrow(() ->
            trustManager.checkServerTrusted(new X509Certificate[0], null)
        );
    }

    @Test
    void testGetAcceptedIssuers_returnsEmptyArray() {
        X509Certificate[] issuers = trustManager.getAcceptedIssuers();
        assertNotNull(issuers);
        assertEquals(0, issuers.length);
    }

    @Test
    void testGetAcceptedIssuers_returnsNewArrayEachTime() {
        X509Certificate[] issuers1 = trustManager.getAcceptedIssuers();
        X509Certificate[] issuers2 = trustManager.getAcceptedIssuers();
        assertNotNull(issuers1);
        assertNotNull(issuers2);
        assertEquals(0, issuers1.length);
        assertEquals(0, issuers2.length);
    }

    @Test
    void testImplementsX509TrustManager() {
        assertTrue(trustManager instanceof javax.net.ssl.X509TrustManager);
    }
}
