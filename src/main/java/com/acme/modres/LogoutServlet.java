package com.acme.modres;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// Removed WebSphere-specific import: com.ibm.websphere.security.WSSecurityHelper
// Replaced with standard servlet session invalidation for container portability (cz-java-0075)

import java.io.IOException;

@WebServlet({ "/logout" })
public class LogoutServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest request,
      HttpServletResponse response) throws IOException {

    try {
      // Replaced WSSecurityHelper.revokeSSOCookies() with standard servlet session invalidation
      // WSSecurityHelper is WebSphere-specific and not available in container environments
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }
      // Clear authentication cookies via standard HTTP response headers
      javax.servlet.http.Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (javax.servlet.http.Cookie cookie : cookies) {
          cookie.setMaxAge(0);
          cookie.setPath("/");
          response.addCookie(cookie);
        }
      }
    } catch (Exception e) {
      System.err.println("[ERROR] Error logging out");
      e.printStackTrace();
    }

    response.sendRedirect("login.jsp");
  }
}
