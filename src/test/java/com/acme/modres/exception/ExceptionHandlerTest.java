package com.acme.modres.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.servlet.ServletException;
import java.util.logging.Logger;

/**
 * Unit tests for ExceptionHandler class.
 */
public class ExceptionHandlerTest {

    private static final Logger logger = Logger.getLogger(ExceptionHandlerTest.class.getName());

    @Test
    void testHandleException_withNullException_throwsServletException() {
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "Error message", logger)
        );
    }

    @Test
    void testHandleException_withNullException_messageInException() {
        String errorMsg = "Test error message";
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, errorMsg, logger)
        );
        assertEquals(errorMsg, ex.getMessage());
    }

    @Test
    void testHandleException_withException_throwsServletException() {
        Exception cause = new RuntimeException("Root cause");
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "Error with cause", logger)
        );
    }

    @Test
    void testHandleException_withException_messageInException() {
        Exception cause = new RuntimeException("Root cause");
        String errorMsg = "Error with cause";
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, errorMsg, logger)
        );
        assertEquals(errorMsg, ex.getMessage());
    }

    @Test
    void testHandleException_withException_causeIsSet() {
        Exception cause = new RuntimeException("Root cause");
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "Error with cause", logger)
        );
        assertNotNull(ex.getCause());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testHandleException_withNullException_noCause() {
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "No cause error", logger)
        );
        assertNull(ex.getCause());
    }

    @Test
    void testHandleException_withIOException_throwsServletException() {
        java.io.IOException cause = new java.io.IOException("IO error");
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "IO error occurred", logger)
        );
    }

    @Test
    void testHandleException_withEmptyMessage_throwsServletException() {
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "", logger)
        );
    }

    @Test
    void testHandleException_withNullMessage_throwsServletException() {
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, null, logger)
        );
    }

    @Test
    void testHandleException_withIllegalArgumentException_causePreserved() {
        IllegalArgumentException cause = new IllegalArgumentException("Bad argument");
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "Illegal argument", logger)
        );
        assertEquals(cause, ex.getCause());
    }
}
