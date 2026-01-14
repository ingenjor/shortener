package com.shortener.unit;

import com.shortener.infra.config.AppConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    void testGetInstance() {
        AppConfig config1 = AppConfig.getInstance();
        AppConfig config2 = AppConfig.getInstance();

        assertSame(config1, config2, "Should return the same instance");
    }

    @Test
    void testGetShortCodeLength() {
        AppConfig config = AppConfig.getInstance();

        int length = config.getShortCodeLength();
        assertTrue(length > 0);
        assertEquals(7, length);
    }

    @Test
    void testGetDefaultTtlHours() {
        AppConfig config = AppConfig.getInstance();

        int ttl = config.getDefaultTtlHours();
        assertTrue(ttl > 0);
        assertEquals(24, ttl);
    }

    @Test
    void testGetDefaultMaxClicks() {
        AppConfig config = AppConfig.getInstance();

        int maxClicks = config.getDefaultMaxClicks();
        assertTrue(maxClicks > 0);
        assertEquals(100, maxClicks);
    }

    @Test
    void testGetGenerationAlgorithm() {
        AppConfig config = AppConfig.getInstance();

        String algorithm = config.getGenerationAlgorithm();
        assertNotNull(algorithm);
        assertFalse(algorithm.isEmpty());
        assertEquals("BASE62", algorithm);
    }

    @Test
    void testIsExpireNotificationEnabled() {
        AppConfig config = AppConfig.getInstance();

        assertTrue(config.isExpireNotificationEnabled());
    }

    @Test
    void testIsLimitNotificationEnabled() {
        AppConfig config = AppConfig.getInstance();

        assertTrue(config.isLimitNotificationEnabled());
    }

    @Test
    void testGetCleanupIntervalMinutes() {
        AppConfig config = AppConfig.getInstance();

        int interval = config.getCleanupIntervalMinutes();
        assertTrue(interval > 0);
        assertEquals(5, interval);
    }

    @Test
    void testIsAutoDeleteExpired() {
        AppConfig config = AppConfig.getInstance();

        assertTrue(config.isAutoDeleteExpired());
    }

    @Test
    void testIsOwnerOnlyOperations() {
        AppConfig config = AppConfig.getInstance();

        assertTrue(config.isOwnerOnlyOperations());
    }

    @Test
    void testGetUserSessionTtlHours() {
        AppConfig config = AppConfig.getInstance();

        int ttl = config.getUserSessionTtlHours();
        assertTrue(ttl > 0);
        assertEquals(168, ttl);
    }

    @Test
    void testGetLoggingLevel() {
        AppConfig config = AppConfig.getInstance();

        String level = config.getLoggingLevel();
        assertNotNull(level);
        assertEquals("INFO", level);
    }

    @Test
    void testGetLoggingFile() {
        AppConfig config = AppConfig.getInstance();

        String file = config.getLoggingFile();
        assertNotNull(file);
        assertEquals("logs/shortener.log", file);
    }
}
