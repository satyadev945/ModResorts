package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {

    @Test
    void testOperation() {
        Service service = new Service();
        assertDoesNotThrow(() -> service.operation());
    }

    @Test
    void testOperationConstant() {
        assertEquals("my-operation", Service.OPERATION);
    }
}
