package com.byteguard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration manager for ByteGuard Discord Bot.
 * Handles environment variables and configuration validation.
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    static {
        try {
            logger.info("Configuration loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load configuration", e);
            throw new RuntimeException("Configuration initialization failed", e);
        }
    }

    // Discord Configuration
    public static String getBotToken() {
        return getRequiredEnv("BOT_TOKEN");
    }

    public static String getClientId() {
        return getRequiredEnv("CLIENT_ID");
    }

    public static String getClientSecret() {
        return getEnv("CLIENT_SECRET", "");
    }

    // Database Configuration
    public static String getDatabaseUrl() {
        return getEnv("DATABASE_URL", "jdbc:h2:mem:byteguard");
    }

    public static String getDatabaseUsername() {
        return getEnv("DATABASE_USERNAME", "byteguard");
    }

    public static String getDatabasePassword() {
        return getEnv("DATABASE_PASSWORD", "");
    }

    // Redis Configuration
    public static String getRedisUrl() {
        return getEnv("REDIS_URL", "redis://localhost:6379");
    }

    // Application Configuration
    public static String getEnvironment() {
        return getEnv("ENVIRONMENT", "development");
    }

    public static boolean isDebugMode() {
        return Boolean.parseBoolean(getEnv("DEBUG_MODE", "false"));
    }

    public static int getDashboardPort() {
        return Integer.parseInt(getEnv("DASHBOARD_PORT", "8080"));
    }

    public static int getThreadPoolSize() {
        return Integer.parseInt(getEnv("THREAD_POOL_SIZE", "5"));
    }

    public static int getMaxGuildsCache() {
        return Integer.parseInt(getEnv("MAX_GUILDS_CACHE", "1000"));
    }

    // Feature Flags
    public static boolean isModerationEnabled() {
        return Boolean.parseBoolean(getEnv("FEATURE_MODERATION", "true"));
    }

    public static boolean isLevelingEnabled() {
        return Boolean.parseBoolean(getEnv("FEATURE_LEVELING", "true"));
    }

    public static boolean isCustomCommandsEnabled() {
        return Boolean.parseBoolean(getEnv("FEATURE_CUSTOM_COMMANDS", "true"));
    }

    public static boolean isAnalyticsEnabled() {
        return Boolean.parseBoolean(getEnv("FEATURE_ANALYTICS", "true"));
    }

    /**
     * Get environment variable with default value.
     *
     * @param key Environment variable key
     * @param defaultValue Default value if not found
     * @return Environment variable value or default
     */
    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get required environment variable.
     *
     * @param key Environment variable key
     * @return Environment variable value
     * @throws IllegalStateException if variable not found
     */
    private static String getRequiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Required environment variable not found: " + key);
        }
        return value;
    }

    /**
     * Validate all required configuration values.
     *
     * @throws IllegalStateException if any required config is missing
     */
    public static void validateRequiredConfig() {
        logger.info("Validating required configuration...");

        try {
            getBotToken();
            getClientId();
            logger.info("✅ All required configuration validated successfully");
        } catch (IllegalStateException e) {
            logger.error("❌ Configuration validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Print current configuration (without sensitive data).
     */
    public static void printConfig() {
        logger.info("=== ByteGuard Configuration ===");
        logger.info("Environment: {}", getEnvironment());
        logger.info("Debug Mode: {}", isDebugMode());
        logger.info("Dashboard Port: {}", getDashboardPort());
        logger.info("Database URL: {}", getDatabaseUrl());
        logger.info("Redis URL: {}", getRedisUrl());
        logger.info("Features - Moderation: {}, Leveling: {}, Custom Commands: {}, Analytics: {}",
                isModerationEnabled(), isLevelingEnabled(), isCustomCommandsEnabled(), isAnalyticsEnabled());
        logger.info("===============================");
    }
}