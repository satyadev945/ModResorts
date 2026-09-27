package com.acme.modres.db;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer information service backed by Amazon ElastiCache (Redis) on EKS.
 *
 * <p>Rule cz-java-0064 (Singleton State Storage): The former @Singleton EJB
 * caused in-process state that becomes inconsistent when multiple container
 * replicas run simultaneously.  State is now externalised into Redis so every
 * pod shares a single, consistent data store.
 *
 * <p>Connection details are supplied via environment variables:
 * <ul>
 *   <li>REDIS_HOST  – ElastiCache primary endpoint (default: localhost)</li>
 *   <li>REDIS_PORT  – Redis port                   (default: 6379)</li>
 * </ul>
 */
@ApplicationScoped
@Startup
public class ModResortsCustomerInformation {

    private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";
    private static final String REDIS_CACHE_KEY        = "modresorts:customer:info";

    // Removing DB connection for ease of demo setup
    // @Resource(lookup = "jdbc/ModResortsJndi")
    private DataSource dataSource;

    /** Shared Redis connection pool – one pool per application instance. */
    private JedisPool jedisPool;

    @PostConstruct
    public void init() {
        String redisHost = System.getenv("REDIS_HOST") != null
                ? System.getenv("REDIS_HOST") : "localhost";
        int redisPort = System.getenv("REDIS_PORT") != null
                ? Integer.parseInt(System.getenv("REDIS_PORT")) : 6379;

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        jedisPool = new JedisPool(poolConfig, redisHost, redisPort);
    }

    @PreDestroy
    public void destroy() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    /**
     * Returns customer information.
     *
     * <p>The list is cached in Redis under {@value #REDIS_CACHE_KEY}.
     * On a cache miss the data is fetched from the relational database and
     * stored back into Redis so subsequent requests (from any pod) are served
     * from the shared cache.
     *
     * @return list of customer info strings
     */
    public ArrayList<String> getCustomerInformation() {
        // --- 1. Try Redis cache first (shared across all EKS pod replicas) ---
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> cached = jedis.lrange(REDIS_CACHE_KEY, 0, -1);
            if (cached != null && !cached.isEmpty()) {
                return new ArrayList<>(cached);
            }
        } catch (Exception e) {
            // Redis unavailable – fall through to DB
            e.printStackTrace();
        }

        // --- 2. Cache miss: query the relational database ---
        ArrayList<String> customerInfo = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(SELECT_CUSTOMERS_QUERY);
            rs   = stmt.executeQuery();

            while (rs.next()) {
                customerInfo.add(rs.getString("INFO"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs   != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // --- 3. Populate Redis cache so all replicas benefit ---
        if (!customerInfo.isEmpty()) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del(REDIS_CACHE_KEY);
                for (String info : customerInfo) {
                    jedis.rpush(REDIS_CACHE_KEY, info);
                }
            } catch (Exception e) {
                // Non-fatal: cache population failure should not break the response
                e.printStackTrace();
            }
        }

        return customerInfo;
    }
}
