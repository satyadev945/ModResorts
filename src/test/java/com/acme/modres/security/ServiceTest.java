package com.acme.modres.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Service class.
 */
public class ServiceTest {

    private Service service;

    @BeforeEach
    void setUp() {
        service = new Service();
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(service);
    }

    @Test
    void testOperationConstant_notNull() {
        assertNotNull(Service.OPERATION);
    }

    @Test
    void testOperationConstant_correctValue() {
        assertEquals("my-operation", Service.OPERATION);
    }

    @Test
    void testOperation_doesNotThrow() {
        assertDoesNotThrow(() -> service.operation());
    }

    @Test
    void testOperation_canBeCalledMultipleTimes() {
        assertDoesNotThrow(() -> {
            service.operation();
            service.operation();
            service.operation();
        });
    }
}
