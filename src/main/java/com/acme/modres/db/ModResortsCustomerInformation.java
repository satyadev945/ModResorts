package com.acme.modres.db;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Customer information service migrated from EJB 2.x (@Singleton/@Startup)
 * to a Spring Boot @Service component.
 *
 * The EJB container-managed lifecycle annotations (javax.ejb.Singleton,
 * javax.ejb.Startup) have been replaced with Spring's @Service stereotype,
 * and the DataSource is injected via Spring's @Autowired mechanism backed
 * by an AWS RDS-compatible HikariCP connection pool configured through
 * application.properties / environment variables.
 */
@Service
public class ModResortsCustomerInformation {

  private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

  // DataSource is now provided by Spring Boot auto-configuration (HikariCP)
  // backed by AWS RDS connection details supplied via environment variables:
  //   SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD
  @Autowired
  private DataSource dataSource;

  public ArrayList<String> getCustomerInformation() {
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    ArrayList<String> customerInfo = new ArrayList<>();

    try {
      // Get a connection from the Spring-managed HikariCP data source
      conn = dataSource.getConnection();
      // Create a prepared statement
      stmt = conn.prepareStatement(SELECT_CUSTOMERS_QUERY);
      // Execute the query
      rs = stmt.executeQuery();

      // Process the results
      while (rs.next()) {
        String info = rs.getString("INFO");
        customerInfo.add(info);
      }

    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      // Close the result set, statement, and connection
      try {
        if (rs != null)
          rs.close();
        if (stmt != null)
          stmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return customerInfo;
  }
}
