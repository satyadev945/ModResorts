package com.acme.modres.db;

import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class ModResortsCustomerInformation {

  private static final Logger logger = Logger.getLogger(ModResortsCustomerInformation.class.getName());

  // PostgreSQL-compatible query using lowercase snake_case identifiers
  private static final String SELECT_CUSTOMERS_QUERY = "SELECT info FROM customer";

  @Resource(lookup = "jdbc/ModResortsJndi")
  private DataSource dataSource;

  public ArrayList<String> getCustomerInformation() {
    ArrayList<String> customerInfo = new ArrayList<>();

    if (dataSource == null) {
      logger.log(Level.WARNING, "DataSource is not available. Returning empty customer list.");
      return customerInfo;
    }

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(SELECT_CUSTOMERS_QUERY);
         ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        String info = rs.getString("info");
        customerInfo.add(info);
      }

    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Error retrieving customer information", e);
    }

    return customerInfo;
  }
}
