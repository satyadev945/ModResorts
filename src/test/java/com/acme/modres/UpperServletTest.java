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

class UpperServletTest {

    @Test
    void testDoGet_WithText() throws ServletException, IOException {
        UpperServlet servlet = new UpperServlet();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        
        when(request.getParameter("text")).thenReturn("hello");
        when(response.getWriter()).thenReturn(writer);
        
        servlet.doGet(request, response);
        
        verify(response).setContentType("text/plain");
        assertEquals("HELLO\n", stringWriter.toString().replace("\r\n", "\n"));
    }

    @Test
    void testDoGet_WithoutText() throws ServletException, IOException {
        UpperServlet servlet = new UpperServlet();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        
        when(request.getParameter("text")).thenReturn(null);
        when(response.getWriter()).thenReturn(writer);
        
        servlet.doGet(request, response);
        
        verify(response).setContentType("text/plain");
        assertEquals("\n", stringWriter.toString().replace("\r\n", "\n"));
    }
}
