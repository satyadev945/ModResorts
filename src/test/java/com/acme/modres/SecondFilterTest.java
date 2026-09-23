package com.acme.modres;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecondFilter class.
 */
@ExtendWith(MockitoExtension.class)
public class SecondFilterTest {

    private SecondFilter secondFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private FilterConfig filterConfig;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws IOException {
        secondFilter = new SecondFilter();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
    }

    @Test
    void testInit_doesNotThrow() {
        assertDoesNotThrow(() -> secondFilter.init(filterConfig));
    }

    @Test
    void testDestroy_doesNotThrow() {
        assertDoesNotThrow(() -> secondFilter.destroy());
    }

    @Test
    void testDoFilter_withContent_appendsToSite() throws IOException, ServletException {
        String requestBody = "Hello";
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        when(response.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(request, response, filterChain);

        String output = stringWriter.toString();
        assertTrue(output.contains("Hello"));
        assertTrue(output.contains("to our site!"));
    }

    @Test
    void testDoFilter_setsContentTypePlain() throws IOException, ServletException {
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("")));
        when(response.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(request, response, filterChain);

        verify(response).setContentType("text/plain");
    }

    @Test
    void testDoFilter_callsChainDoFilter() throws IOException, ServletException {
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("test")));
        when(response.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilter_withEmptyBody_appendsToSite() throws IOException, ServletException {
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("")));
        when(response.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(request, response, filterChain);

        String output = stringWriter.toString();
        assertTrue(output.contains("to our site!"));
    }

    @Test
    void testDoFilter_withMultilineContent_joinsLines() throws IOException, ServletException {
        String requestBody = "line1\nline2";
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        when(response.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(request, response, filterChain);

        String output = stringWriter.toString();
        assertTrue(output.contains("line1"));
        assertTrue(output.contains("line2"));
    }

    @Test
    void testInit_withNullFilterConfig_doesNotThrow() {
        assertDoesNotThrow(() -> secondFilter.init(null));
    }
}
