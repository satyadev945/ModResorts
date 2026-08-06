package com.acme.modres.exception;

import org.junit.jupiter.api.Test;
import jakarta.servlet.ServletException;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class ExceptionHandlerTest {

    @Test
    void testHandle() {
        ExceptionHandler handler = new ExceptionHandler();
        Exception e = new Exception("Test exception");
        assertDoesNotThrow(() -> handler.handle(e));
    }
}
