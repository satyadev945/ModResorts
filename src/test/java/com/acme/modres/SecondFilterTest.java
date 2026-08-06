package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SecondFilterTest {

    @Test
    void testDoFilter_CorrectContextPath() throws IOException, ServletException {
        SecondFilter filter = new SecondFilter();
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        
        when(httpRequest.getContextPath()).thenReturn("/resorts");
        
        filter.doFilter((ServletRequest) httpRequest, response, chain);
        
        verify(chain).doFilter(httpRequest, response);
    }

    @Test
    void testDoFilter_IncorrectContextPath() throws IOException, ServletException {
        SecondFilter filter = new SecondFilter();
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        
        when(httpRequest.getContextPath()).thenReturn("/wrong");
        
        filter.doFilter((ServletRequest) httpRequest, (ServletResponse) httpResponse, chain);
        
        verify(httpResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void testInit() {
        SecondFilter filter = new SecondFilter();
        assertDoesNotThrow(() -> filter.init(mock(FilterConfig.class)));
    }

    @Test
    void testDestroy() {
        SecondFilter filter = new SecondFilter();
        assertDoesNotThrow(() -> filter.destroy());
    }
}
