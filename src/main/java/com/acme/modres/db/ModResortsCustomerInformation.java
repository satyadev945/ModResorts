package com.acme.modres.db;

import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Customer information service backed by Azure Cache for Redis on AKS.
 *
 * <p>The JVM-local @Singleton state has been replaced with an external Redis cache
 * to ensure consistency when the application is scaled horizontally across multiple
 * container replicas. Connection details are injected via environment variables
 * (populated by the Azure Key Vault CSI driver in AKS).</p>
 *
 * <p>Required environment variables:
 * <ul>
 *   <li>REDIS_HOST  – Azure Cache for Redis hostname (e.g. my-cache.redis.cache.windows.net)</li>
 *   <li>REDIS_PORT  – Redis port (default: 6380 for Azure Cache for Redis SSL)</li>
 *   <li>REDIS_PASSWORD – Azure Cache for Redis access key</li>
 *   <li>REDIS_SSL   – "true" to enable SSL/TLS (recommended for Azure Cache for Redis)</li>
 * </ul>
 * </p>
 */
@Stateless
public class ModResortsCustomerInformation {

    private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

    /** Redis key under which the customer list is cached. */
    private static final String REDIS_CACHE_KEY = "modresorts:customer:info";

    // Removing DB connection for ease of demo setup
    // @Resource(lookup = "jdbc/ModResortsJndi")
    private DataSource dataSource;

    // -----------------------------------------------------------------------
    // Redis connection pool – configured entirely from environment variables
    // injected by the Azure Key Vault CSI driver into the AKS pod.
    // -----------------------------------------------------------------------
    private static final JedisPool jedisPool;

    static {
        String redisHost     = System.getenv("REDIS_HOST");
        String redisPortStr  = System.getenv("REDIS_PORT");
        String redisPassword = System.getenv("REDIS_PASSWORD");
        String redisSslStr   = System.getenv("REDIS_SSL");

        // Provide safe defaults so the application can still start in
        // environments where Redis is not yet configured.
        if (redisHost == null || redisHost.isEmpty()) {
            redisHost = "localhost";
        }
        int redisPort = (redisPortStr != null && !redisPortStr.isEmpty())
                ? Integer.parseInt(redisPortStr) : 6380;
        boolean redisSsl = !"false".equalsIgnoreCase(redisSslStr);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, redisHost, redisPort,
                    2000, redisPassword, redisSsl);
        } else {
            jedisPool = new JedisPool(poolConfig, redisHost, redisPort,
                    2000, null, redisSsl);
        }
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns customer information, reading from Azure Cache for Redis when
     * available and falling back to the relational database on a cache miss.
     *
     * @return list of customer info strings
     */
    public ArrayList<String> getCustomerInformation() {
        // 1. Try to serve from Redis cache first
        ArrayList<String> cached = getFromRedisCache();
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 2. Cache miss – query the database and populate the cache
        ArrayList<String> customerInfo = fetchFromDatabase();
        if (!customerInfo.isEmpty()) {
            storeInRedisCache(customerInfo);
        }
        return customerInfo;
    }

    // -----------------------------------------------------------------------
    // Redis helpers
    // -----------------------------------------------------------------------

    private ArrayList<String> getFromRedisCache() {
        try (Jedis jedis = jedisPool.getResource()) {
            java.util.List<String> cached = jedis.lrange(REDIS_CACHE_KEY, 0, -1);
            if (cached != null && !cached.isEmpty()) {
                return new ArrayList<>(cached);
            }
        } catch (Exception e) {
            // Log and fall through to database on Redis unavailability
            e.printStackTrace();
        }
        return null;
    }

    private void storeInRedisCache(ArrayList<String> customerInfo) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Delete any stale list before repopulating
            jedis.del(REDIS_CACHE_KEY);
            for (String info : customerInfo) {
                jedis.rpush(REDIS_CACHE_KEY, info);
            }
            // Cache TTL: 5 minutes (300 seconds)
            jedis.expire(REDIS_CACHE_KEY, 300);
        } catch (Exception e) {
            // Non-fatal: application continues without caching
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------
    // Database helpers
    // -----------------------------------------------------------------------

    private ArrayList<String> fetchFromDatabase() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList<String> customerInfo = new ArrayList<>();

        try {
            // Get a connection from the injected data source
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
