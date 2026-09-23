package com.acme.modres.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.net.ssl.SSLContext;

/**
 * Unit tests for SSLUtils class.
 */
public class SSLUtilsTest {

    private SSLUtils sslUtils;

    @BeforeEach
    void setUp() {
        sslUtils = new SSLUtils();
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(sslUtils);
    }

    @Test
    void testGetContext_returnsNonNull() throws Exception {
        SSLContext context = sslUtils.getContext();
        assertNotNull(context);
    }

    @Test
    void testGetContext_returnsTLSContext() throws Exception {
        SSLContext context = sslUtils.getContext();
        assertNotNull(context);
        assertEquals("TLS", context.getProtocol());
    }

    @Test
    void testGetContext_canBeCalledMultipleTimes() throws Exception {
        SSLContext context1 = sslUtils.getContext();
        SSLContext context2 = sslUtils.getContext();
        assertNotNull(context1);
        assertNotNull(context2);
    }

    @Test
    void testGetContext_returnsInitializedContext() throws Exception {
        SSLContext context = sslUtils.getContext();
        assertNotNull(context);
        // Verify the context is initialized by checking it can create a socket factory
        assertNotNull(context.getSocketFactory());
    }
}
