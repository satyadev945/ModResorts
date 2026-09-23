package com.acme.modres;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LogoutServlet class.
 */
@ExtendWith(MockitoExtension.class)
public class LogoutServletTest {

    private LogoutServlet logoutServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp() {
        logoutServlet = new LogoutServlet();
    }

    @Test
    void testDoGet_invalidatesSession() throws IOException, ServletException {
        when(request.getSession()).thenReturn(session);

        logoutServlet.doGet(request, response);

        verify(session).invalidate();
    }

    @Test
    void testDoGet_redirectsToLoginPage() throws IOException, ServletException {
        when(request.getSession()).thenReturn(session);

        logoutServlet.doGet(request, response);

        verify(response).sendRedirect("login.jsp");
    }

    @Test
    void testDoGet_callsGetSession() throws IOException, ServletException {
        when(request.getSession()).thenReturn(session);

        logoutServlet.doGet(request, response);

        verify(request).getSession();
    }

    @Test
    void testDoGet_doesNotThrow() {
        when(request.getSession()).thenReturn(session);

        assertDoesNotThrow(() -> logoutServlet.doGet(request, response));
    }

    @Test
    void testDoGet_invalidatesBeforeRedirect() throws IOException, ServletException {
        when(request.getSession()).thenReturn(session);

        logoutServlet.doGet(request, response);

        // Verify both operations happened
        verify(session, times(1)).invalidate();
        verify(response, times(1)).sendRedirect("login.jsp");
    }
}
