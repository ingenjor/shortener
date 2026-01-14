package com.shortener.integration;

import com.shortener.core.domain.Link;
import com.shortener.core.service.NotificationService;
import com.shortener.infra.scheduler.LinkCleanupScheduler;
import com.shortener.infra.storage.InMemoryLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkCleanupSchedulerTest {
    private InMemoryLinkRepository repository;
    private NotificationService notificationService;
    private LinkCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryLinkRepository();
        notificationService = new NotificationService();
        scheduler = new LinkCleanupScheduler(
                repository,
                notificationService,
                true,  // auto delete
                1      // check every minute for testing
        );
    }

    @Test
    void testCleanupExpiredLinks() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        // Create expired link with protected constructor (for testing)
        Link expiredLink = createLinkForTest(
                userId,
                "https://expired.com",
                "expired123",
                now.minusHours(2), // created 2 hours ago
                now.minusHours(1), // expired 1 hour ago
                10,
                0,
                true,
                "Expired"
        );

        // Create active link
        Link activeLink = createLinkForTest(
                userId,
                "https://active.com",
                "active456",
                now.minusMinutes(30), // created 30 minutes ago
                now.plusHours(1),     // expires in 1 hour
                10,
                0,
                true,
                "Active"
        );

        repository.save(expiredLink);
        repository.save(activeLink);

        assertEquals(2, repository.count());

        // Use reflection to call private method
        Method method = LinkCleanupScheduler.class.getDeclaredMethod("cleanupExpiredLinks");
        method.setAccessible(true);
        method.invoke(scheduler);

        // Only active link should remain
        assertEquals(1, repository.count());
        assertTrue(repository.findByShortCode("active456").isPresent());
        assertFalse(repository.findByShortCode("expired123").isPresent());
    }

    @Test
    void testCleanupNoExpiredLinks() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Link activeLink = createLinkForTest(
                userId,
                "https://active.com",
                "active123",
                now.minusMinutes(30),
                now.plusHours(1),
                10,
                0,
                true,
                "Active"
        );

        repository.save(activeLink);

        assertEquals(1, repository.count());

        // Use reflection to call private method
        Method method = LinkCleanupScheduler.class.getDeclaredMethod("cleanupExpiredLinks");
        method.setAccessible(true);
        method.invoke(scheduler);

        // Link should still exist
        assertEquals(1, repository.count());
        assertTrue(repository.findByShortCode("active123").isPresent());
    }

    // Helper method to create Link for testing with custom timestamps
    private Link createLinkForTest(UUID userId, String originalUrl, String shortCode,
                                   LocalDateTime createdAt, LocalDateTime expiresAt,
                                   int maxClicks, int currentClicks, boolean isActive, String description) {
        try {
            // Use reflection to access protected constructor
            var constructor = Link.class.getDeclaredConstructor(
                    UUID.class, String.class, String.class, LocalDateTime.class, LocalDateTime.class,
                    Integer.TYPE, Integer.TYPE, Boolean.TYPE, String.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    userId, originalUrl, shortCode, createdAt, expiresAt,
                    maxClicks, currentClicks, isActive, description
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test link", e);
        }
    }
}
