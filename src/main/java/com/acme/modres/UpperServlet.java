package com.acme.modres;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/resorts/upper")
public class UpperServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String text = request.getParameter("text");
    if (text == null) {
      text = "";
    }
    String upperText = text.toUpperCase();
    response.setContentType("text/plain");
    PrintWriter out = response.getWriter();
    out.println(upperText);
  }
}
