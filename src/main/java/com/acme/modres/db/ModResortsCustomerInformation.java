package com.acme.modres.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public class ModResortsCustomerInformation {
  private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";
  private final DataSource dataSource;

  public ModResortsCustomerInformation() {
    this.dataSource = createDataSource();
  }

  public ArrayList<String> getCustomerInformation() {
    ArrayList<String> customerInfo = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(SELECT_CUSTOMERS_QUERY);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        customerInfo.add(rs.getString("INFO"));
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Unable to retrieve customer information", e);
    }
    return customerInfo;
  }

  private DataSource createDataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(requiredEnv("MODRESORTS_DB_URL"));
    config.setUsername(requiredEnv("MODRESORTS_DB_USERNAME"));
    config.setPassword(requiredEnv("MODRESORTS_DB_PASSWORD"));
    config.setMaximumPoolSize(Integer.parseInt(System.getenv().getOrDefault("MODRESORTS_DB_MAX_POOL_SIZE", "10")));
    config.setMinimumIdle(Integer.parseInt(System.getenv().getOrDefault("MODRESORTS_DB_MIN_IDLE", "2")));
    config.setPoolName("modresorts-hikari-pool");
    return new HikariDataSource(config);
  }

  private String requiredEnv(String key) {
    String value = System.getenv(key);
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalStateException("Missing required database configuration: " + key);
    }
    return value;
  }
}
