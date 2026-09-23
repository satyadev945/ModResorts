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
 * Unit tests for WelcomeServlet class.
 */
@ExtendWith(MockitoExtension.class)
public class WelcomeServletTest {

    private WelcomeServlet welcomeServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws IOException {
        welcomeServlet = new WelcomeServlet();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
    }

    @Test
    void testDoGet_setsContentType() throws IOException, ServletException {
        when(response.getWriter()).thenReturn(printWriter);

        welcomeServlet.doGet(request, response);

        verify(response).setContentType("text/plain");
    }

    @Test
    void testDoGet_writesEnjoyMessage() throws IOException, ServletException {
        when(response.getWriter()).thenReturn(printWriter);

        welcomeServlet.doGet(request, response);

        String output = stringWriter.toString();
        assertTrue(output.contains("Enjoy!"));
    }

    @Test
    void testDoGet_doesNotThrow() throws IOException, ServletException {
        when(response.getWriter()).thenReturn(printWriter);

        welcomeServlet.doGet(request, response);

        assertNotNull(stringWriter.toString());
    }

    @Test
    void testDoGet_callsGetWriter() throws IOException, ServletException {
        when(response.getWriter()).thenReturn(printWriter);

        welcomeServlet.doGet(request, response);

        verify(response).getWriter();
    }
}
