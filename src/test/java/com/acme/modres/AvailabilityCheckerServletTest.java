package com.acme.modres;

import org.junit.jupiter.api.Test;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AvailabilityCheckerServletTest {

    @Test
    void testDoGet() throws ServletException, IOException {
        AvailabilityCheckerServlet servlet = new AvailabilityCheckerServlet();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        
        when(response.getWriter()).thenReturn(writer);
        
        servlet.doGet(request, response);
        
        verify(response).setContentType("text/plain");
        assertTrue(stringWriter.toString().contains("Availability is high"));
    }
}
