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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FirstFilter class.
 */
@ExtendWith(MockitoExtension.class)
public class FirstFilterTest {

    private FirstFilter firstFilter;

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
        firstFilter = new FirstFilter();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
    }

    @Test
    void testInit_doesNotThrow() {
        assertDoesNotThrow(() -> firstFilter.init(filterConfig));
    }

    @Test
    void testDestroy_doesNotThrow() {
        assertDoesNotThrow(() -> firstFilter.destroy());
    }

    @Test
    void testDoFilter_withUser_writesWelcomeMessage() throws IOException, ServletException {
        when(request.getParameter("user")).thenReturn("Alice");
        when(response.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(request, response, filterChain);

        verify(response).setContentType("text/plain");
        String output = stringWriter.toString();
        assertTrue(output.contains("Welcome Alice"));
    }

    @Test
    void testDoFilter_withNullUser_usesDefaultUser() throws IOException, ServletException {
        when(request.getParameter("user")).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(request, response, filterChain);

        String output = stringWriter.toString();
        assertTrue(output.contains("Welcome defaultUser"));
    }

    @Test
    void testDoFilter_callsChainDoFilter() throws IOException, ServletException {
        when(request.getParameter("user")).thenReturn("Bob");
        when(response.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilter_setsContentTypePlain() throws IOException, ServletException {
        when(request.getParameter("user")).thenReturn("Charlie");
        when(response.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(request, response, filterChain);

        verify(response).setContentType("text/plain");
    }

    @Test
    void testDoFilter_withEmptyUser_writesWelcomeEmpty() throws IOException, ServletException {
        when(request.getParameter("user")).thenReturn("");
        when(response.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(request, response, filterChain);

        String output = stringWriter.toString();
        assertTrue(output.contains("Welcome "));
    }

    @Test
    void testInit_withNullFilterConfig_doesNotThrow() {
        assertDoesNotThrow(() -> firstFilter.init(null));
    }
}
