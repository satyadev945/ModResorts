package com.acme.modres;

import org.junit.jupiter.api.Test;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class LogoutServletTest {

    @Test
    void testDoGet() throws IOException {
        LogoutServlet servlet = new LogoutServlet();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        
        when(request.getSession()).thenReturn(session);
        when(response.getWriter()).thenReturn(writer);
        
        servlet.doGet(request, response);
        
        verify(session).invalidate();
        verify(response).setContentType("text/plain");
        assertTrue(stringWriter.toString().contains("Logged out successfully"));
    }
}
