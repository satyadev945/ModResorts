package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SSLUtilsTest {

    @Test
    void testInstantiation() {
        SSLUtils sslUtils = new SSLUtils();
        assertNotNull(sslUtils);
    }
}
