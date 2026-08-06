package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FakeX509TrustManagerTest {

    @Test
    void testConstructor() {
        FakeX509TrustManager trustManager = new FakeX509TrustManager();
        assertNotNull(trustManager);
    }
}
