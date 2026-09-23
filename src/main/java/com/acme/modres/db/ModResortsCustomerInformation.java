package com.acme.modres.db;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Replaces the EJB @Singleton/@Startup singleton state storage pattern with a
 * Google Cloud Memorystore (Redis) backed implementation on GKE Autopilot.
 *
 * Connection details are injected via environment variables:
 *   REDIS_HOST - Memorystore Redis host (default: localhost)
 *   REDIS_PORT - Memorystore Redis port (default: 6379)
 *
 * This eliminates JVM-level singleton state that causes data inconsistencies
 * when the application is scaled horizontally across multiple container replicas.
 *
 * Rule: cz-java-0064 (Singleton State Storage)
 */
public class ModResortsCustomerInformation {

    private static final Logger LOGGER = Logger.getLogger(ModResortsCustomerInformation.class.getName());

    private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

    /** Redis key under which customer information list is stored. */
    private static final String REDIS_CUSTOMER_KEY = "modresorts:customer:info";

    /** Memorystore Redis host injected via environment variable. */
    private static final String REDIS_HOST =
            System.getenv("REDIS_HOST") != null ? System.getenv("REDIS_HOST") : "localhost";

    /** Memorystore Redis port injected via environment variable. */
    private static final int REDIS_PORT;

    static {
        int port = 6379;
        String portEnv = System.getenv("REDIS_PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid REDIS_PORT value ''{0}'', using default 6379", portEnv);
            }
        }
        REDIS_PORT = port;
    }

    private final JedisPool jedisPool;

    public ModResortsCustomerInformation() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        this.jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT);
        LOGGER.log(Level.INFO,
                "ModResortsCustomerInformation initialised with Memorystore Redis at {0}:{1}",
                new Object[]{REDIS_HOST, REDIS_PORT});
    }

    /**
     * Retrieves customer information from Google Cloud Memorystore (Redis).
     * Falls back to an empty list if the key is not present or Redis is unavailable.
     *
     * @return list of customer information strings stored in Redis
     */
    public ArrayList<String> getCustomerInformation() {
        ArrayList<String> customerInfo = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> values = jedis.lrange(REDIS_CUSTOMER_KEY, 0, -1);
            if (values != null) {
                customerInfo.addAll(values);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to retrieve customer information from Memorystore Redis at "
                            + REDIS_HOST + ":" + REDIS_PORT, e);
        }
        return customerInfo;
    }

    /**
     * Stores a customer information entry in Google Cloud Memorystore (Redis).
     *
     * @param info the customer information string to persist
     */
    public void addCustomerInformation(String info) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.rpush(REDIS_CUSTOMER_KEY, info);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to store customer information in Memorystore Redis at "
                            + REDIS_HOST + ":" + REDIS_PORT, e);
        }
    }

    /**
     * Closes the underlying Jedis connection pool. Should be called on application shutdown.
     */
    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}
