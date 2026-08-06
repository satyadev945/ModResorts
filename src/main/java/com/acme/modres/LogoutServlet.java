package com.acme.modres;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/resorts/logout")
public class LogoutServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    request.getSession().invalidate();
    response.setContentType("text/plain");
    PrintWriter out = response.getWriter();
    out.println("Logged out successfully");
  }
}
