package com.acme.modres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;

/**
 * Entry point for running ModResorts as an executable JAR with an embedded
 * Tomcat servlet container (no external application server required).
 * The server port is configured through the PORT/SERVER_PORT environment variable
 * (see application.properties).
 */
@SpringBootApplication
@ServletComponentScan(basePackages = "com.acme.modres")
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  // Filters that were previously registered in WEB-INF/web.xml
  @Bean
  public FilterRegistrationBean<FirstFilter> firstFilterRegistration() {
    FilterRegistrationBean<FirstFilter> registration = new FilterRegistrationBean<>(new FirstFilter());
    registration.addUrlPatterns("/welcome");
    registration.setOrder(1);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<SecondFilter> secondFilterRegistration() {
    FilterRegistrationBean<SecondFilter> registration = new FilterRegistrationBean<>(new SecondFilter());
    registration.addUrlPatterns("/welcome");
    registration.setOrder(2);
    return registration;
  }
}
