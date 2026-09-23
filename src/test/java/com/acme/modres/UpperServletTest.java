package com.acme.modres;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UpperServlet class.
 */
@ExtendWith(MockitoExtension.class)
public class UpperServletTest {

    private UpperServlet upperServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws IOException {
        upperServlet = new UpperServlet();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
    }

    @Test
    void testDoGet_setsContentTypeHtml() throws IOException, ServletException {
        when(request.getParameter("input")).thenReturn("hello");
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        verify(response).setContentType("text/html");
    }

    @Test
    void testDoGet_convertsToUpperCase() throws IOException, ServletException {
        when(request.getParameter("input")).thenReturn("hello");
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        String output = stringWriter.toString();
        assertTrue(output.contains("HELLO"));
    }

    @Test
    void testDoGet_withNullInput_usesEmptyString() throws IOException, ServletException {
        when(request.getParameter("input")).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        String output = stringWriter.toString();
        assertNotNull(output);
        // Should not throw and should produce output
        assertTrue(output.contains("upper case input"));
    }

    @Test
    void testDoGet_withMixedCase_convertsAll() throws IOException, ServletException {
        when(request.getParameter("input")).thenReturn("HeLLo WoRLd");
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        String output = stringWriter.toString();
        assertTrue(output.contains("HELLO WORLD"));
    }

    @Test
    void testDoGet_withNumbers_preservesNumbers() throws IOException, ServletException {
        when(request.getParameter("input")).thenReturn("test123");
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        String output = stringWriter.toString();
        assertTrue(output.contains("TEST123"));
    }

    @Test
    void testDoGet_withEmptyString_producesEmptyUpperCase() throws IOException, ServletException {
        when(request.getParameter("input")).thenReturn("");
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        String output = stringWriter.toString();
        assertNotNull(output);
    }

    @Test
    void testDoGet_doesNotThrow() throws IOException, ServletException {
        when(request.getParameter("input")).thenReturn("test");
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        assertNotNull(stringWriter.toString());
    }

    @Test
    void testDoGet_outputContainsUpperCaseLabel() throws IOException, ServletException {
        when(request.getParameter("input")).thenReturn("world");
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        String output = stringWriter.toString();
        assertTrue(output.contains("upper case input"));
    }
}
