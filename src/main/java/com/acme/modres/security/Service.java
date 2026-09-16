package com.acme.modres.security;

public class Service {
  public static final String OPERATION = "my-operation";

  public void operation() {
    // SecurityManager was deprecated in Java 17 and removed in Java 21.
    // Access control should be handled via proper authentication/authorization
    // frameworks (e.g., Spring Security, Jakarta Security) instead.
    System.out.println("Operation is executed");
  }
}
