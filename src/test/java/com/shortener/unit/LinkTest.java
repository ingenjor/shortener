package com.shortener.unit;

import com.shortener.core.domain.Link;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkTest {

    @Test
    void testCreateLinkWithValidUrl() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(
                userId,
                "https://example.com",
                "abc123",
                expiresAt,
                100,
                "Test link"
        );

        assertNotNull(link);
        assertEquals(userId, link.getUserId());
        assertEquals("https://example.com", link.getOriginalUrl());
        assertEquals("abc123", link.getShortCode());
        assertEquals(expiresAt, link.getExpiresAt());
        assertEquals(100, link.getMaxClicks());
        assertEquals(0, link.getCurrentClicks());
        assertTrue(link.isActive());
        assertEquals("Test link", link.getDescription());
    }

    @Test
    void testCreateLinkWithInvalidUrlThrowsException() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        assertThrows(IllegalArgumentException.class, () ->
                new Link(userId, "invalid-url", "abc123", expiresAt, 100, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new Link(userId, "", "abc123", expiresAt, 100, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new Link(userId, null, "abc123", expiresAt, 100, null)
        );
    }

    @Test
    void testCreateLinkWithFtpUrl() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(
                userId,
                "ftp://example.com/file.txt",
                "ftp123",
                expiresAt,
                50,
                "FTP link"
        );

        assertEquals("ftp://example.com/file.txt", link.getOriginalUrl());
    }

    @Test
    void testIncrementClicksIncrementsCounter() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(userId, "https://example.com", "abc123", expiresAt, 10, null);

        assertEquals(0, link.getCurrentClicks());
        assertDoesNotThrow(link::incrementClicks);
        assertEquals(1, link.getCurrentClicks());
    }

    @Test
    void testIncrementClicksWhenInactiveThrowsException() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Link link = createTestLink(userId, "https://example.com", "abc123",
                now.minusHours(2), now.minusHours(1), 10, 0, false, "Inactive");

        assertThrows(IllegalStateException.class, link::incrementClicks);
    }

    @Test
    void testIncrementClicksWhenExpiredDeactivatesLink() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Link link = createTestLink(userId, "https://example.com", "abc123",
                now.minusHours(2), now.minusMinutes(1), 10, 0, true, "Expired");

        assertTrue(link.isExpired());
        assertThrows(IllegalStateException.class, link::incrementClicks);
        assertFalse(link.isActive());
    }

    @Test
    void testIncrementClicksWhenLimitReached() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(userId, "https://example.com", "abc123", expiresAt, 1, null);

        // First click should work
        assertDoesNotThrow(link::incrementClicks);
        assertEquals(1, link.getCurrentClicks());
        assertFalse(link.isActive());

        // Second click should throw
        assertThrows(IllegalStateException.class, link::incrementClicks);
    }

    @Test
    void testUpdateMaxClicksValid() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(userId, "https://example.com", "abc123", expiresAt, 10, null);

        link.updateMaxClicks(20);
        assertEquals(20, link.getMaxClicks());

        link.updateMaxClicks(50);
        assertEquals(50, link.getMaxClicks());
    }

    @Test
    void testUpdateMaxClicksInvalidThrowsException() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(userId, "https://example.com", "abc123", expiresAt, 10, null);

        // Make some clicks
        link.incrementClicks();
        link.incrementClicks();

        assertThrows(IllegalArgumentException.class, () -> link.updateMaxClicks(0));
        assertThrows(IllegalArgumentException.class, () -> link.updateMaxClicks(-5));
        assertThrows(IllegalArgumentException.class, () -> link.updateMaxClicks(1)); // Less than current clicks (2)
        assertThrows(IllegalArgumentException.class, () -> link.updateMaxClicks(1_000_001)); // Exceeds max
    }

    @Test
    void testDeactivate() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(userId, "https://example.com", "abc123", expiresAt, 10, null);

        assertTrue(link.isActive());
        link.deactivate();
        assertFalse(link.isActive());
    }

    @Test
    void testIsExpired() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Link expiredLink = createTestLink(userId, "https://expired.com", "exp123",
                now.minusHours(2), now.minusHours(1), 10, 0, true, "Expired");

        Link activeLink = createTestLink(userId, "https://active.com", "act123",
                now.minusHours(1), now.plusHours(1), 10, 0, true, "Active");

        assertTrue(expiredLink.isExpired());
        assertFalse(activeLink.isExpired());
    }

    @Test
    void testHasReachedLimit() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(userId, "https://example.com", "abc123", expiresAt, 3, null);

        assertFalse(link.hasReachedLimit());
        link.incrementClicks();
        assertFalse(link.hasReachedLimit());
        link.incrementClicks();
        assertFalse(link.hasReachedLimit());
        link.incrementClicks();
        assertTrue(link.hasReachedLimit());
    }

    @Test
    void testCanBeAccessed() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Link activeLink = createTestLink(userId, "https://active.com", "act123",
                now.minusHours(1), now.plusHours(1), 10, 5, true, "Active");

        Link expiredLink = createTestLink(userId, "https://expired.com", "exp123",
                now.minusHours(2), now.minusHours(1), 10, 5, true, "Expired");

        Link limitReachedLink = createTestLink(userId, "https://limit.com", "lim123",
                now.minusHours(1), now.plusHours(1), 10, 10, false, "Limit Reached");

        Link inactiveLink = createTestLink(userId, "https://inactive.com", "inact123",
                now.minusHours(1), now.plusHours(1), 10, 5, false, "Inactive");

        assertTrue(activeLink.canBeAccessed());
        assertFalse(expiredLink.canBeAccessed());
        assertFalse(limitReachedLink.canBeAccessed());
        assertFalse(inactiveLink.canBeAccessed());
    }

    @Test
    void testEqualsAndHashCode() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link1 = new Link(userId, "https://example.com", "abc123", expiresAt, 10, null);
        Link link2 = new Link(userId, "https://example2.com", "def456", expiresAt, 20, null);

        assertEquals(link1, link1);
        assertNotEquals(link1, link2);
        assertNotEquals(link1, null);
        assertNotEquals(link1, "string");

        assertEquals(link1.hashCode(), link1.hashCode());
    }

    @Test
    void testToString() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(userId, "https://example.com", "abc123", expiresAt, 10, "Test");

        String str = link.toString();
        assertTrue(str.contains("abc123"));
        assertTrue(str.contains("example.com"));
    }

    @Test
    void testToDetailedString() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(userId, "https://example.com", "abc123", expiresAt, 10, "Test description");

        String detailed = link.toDetailedString();
        assertTrue(detailed.contains("Short Code: abc123"));
        assertTrue(detailed.contains("Original URL: https://example.com"));
        assertTrue(detailed.contains("Test description"));
    }

    @Test
    void testGetStatusDescription() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Link activeLink = createTestLink(userId, "https://active.com", "act123",
                now.minusHours(1), now.plusHours(1), 10, 5, true, "Active");

        Link expiredLink = createTestLink(userId, "https://expired.com", "exp123",
                now.minusHours(2), now.minusHours(1), 10, 5, true, "Expired");

        Link limitLink = createTestLink(userId, "https://limit.com", "lim123",
                now.minusHours(1), now.plusHours(1), 10, 10, false, "Limit");

        Link inactiveLink = createTestLink(userId, "https://inactive.com", "inact123",
                now.minusHours(1), now.plusHours(1), 10, 5, false, "Inactive");

        // Используем contains для проверки, так как могут быть дополнительные слова
        assertTrue(activeLink.getStatusDescription().contains("ACTIVE"));
        assertTrue(expiredLink.getStatusDescription().contains("EXPIRED"));
        assertTrue(limitLink.getStatusDescription().contains("LIMIT REACHED"));
        assertTrue(inactiveLink.getStatusDescription().contains("INACTIVE"));
    }

    @Test
    void testIsOwnedBy() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(userId1, "https://example.com", "abc123", expiresAt, 10, null);

        assertTrue(link.isOwnedBy(userId1));
        assertFalse(link.isOwnedBy(userId2));
    }

    @Test
    void testGetUsagePercentage() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(userId, "https://example.com", "abc123", expiresAt, 10, null);

        assertEquals(0.0, link.getUsagePercentage());

        link.incrementClicks();
        assertEquals(10.0, link.getUsagePercentage());

        link.incrementClicks();
        link.incrementClicks();
        assertEquals(30.0, link.getUsagePercentage());

        for (int i = 0; i < 7; i++) {
            try {
                link.incrementClicks();
            } catch (Exception e) {
                // Ignore limit reached
            }
        }

        assertEquals(100.0, link.getUsagePercentage());
    }

    @Test
    void testGetHoursRemaining() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Link link = createTestLink(userId, "https://example.com", "abc123",
                now.minusHours(1), now.plusHours(5), 10, 0, true, "Test");

        long hoursRemaining = link.getHoursRemaining();
        assertTrue(hoursRemaining >= 4 && hoursRemaining <= 5);
    }

    @Test
    void testIsExpiringSoon() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        // Ссылка, которая истекает через 30 минут (0.5 часа)
        Link expiringSoon = createTestLink(userId, "https://soon.com", "soon123",
                now.minusHours(23), now.plusMinutes(30), 10, 0, true, "Expiring soon");

        // Ссылка, которая истекает через 10 часов
        Link notExpiringSoon = createTestLink(userId, "https://notsoon.com", "notsoon123",
                now.minusHours(1), now.plusHours(10), 10, 0, true, "Not expiring soon");

        // 30 минут <= 1 час (60 минут)
        assertTrue(expiringSoon.isExpiringSoon(1),
                "Ссылка, истекающая через 30 минут, должна считаться истекающей скоро при пороге 1 час");
        assertFalse(notExpiringSoon.isExpiringSoon(1),
                "Ссылка, истекающая через 10 часов, не должна считаться истекающей скоро при пороге 1 час");
    }

    @Test
    void testIsNearLimit() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        Link link = new Link(userId, "https://example.com", "abc123", expiresAt, 10, null);

        assertFalse(link.isNearLimit(80));

        for (int i = 0; i < 8; i++) {
            try {
                link.incrementClicks();
            } catch (Exception e) {
                // Ignore
            }
        }

        assertTrue(link.isNearLimit(80));
        assertTrue(link.isNearLimit(75));
        assertFalse(link.isNearLimit(90));
    }

    // Helper method to create Link for testing with custom timestamps
    private Link createTestLink(UUID userId, String originalUrl, String shortCode,
                                LocalDateTime createdAt, LocalDateTime expiresAt,
                                int maxClicks, int currentClicks, boolean isActive, String description) {
        try {
            Constructor<Link> constructor = Link.class.getDeclaredConstructor(
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
