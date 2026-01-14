package com.shortener.unit;

import com.shortener.core.domain.Link;
import com.shortener.core.domain.User;
import com.shortener.core.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {
    private NotificationService notificationService;
    private ByteArrayOutputStream outputStream;
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testShowErrorMessage() {
        notificationService.showErrorMessage("Test error message");

        String output = outputStream.toString();

        boolean containsError = output.contains("ERROR");
        boolean containsMessage = output.contains("Test error message");
        boolean containsRedX = output.contains("❌");

        assertTrue(containsError || containsMessage || containsRedX,
                "Output should contain 'ERROR', '❌', or 'Test error message'. " +
                        "Actual output: '" + output + "'");
    }

    @Test
    void testNotifyLinkCreated() {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        Link link = createTestLink(
                user.getId(),
                "https://example.com",
                "abc123",
                now.minusMinutes(5),
                now.plusHours(24),
                100,
                0,
                true,
                "Test link"
        );

        notificationService.notifyLinkCreated(user, link, 24);

        String output = outputStream.toString();
        assertTrue(output.contains("LINK CREATED SUCCESSFULLY") ||
                output.contains("https://example.com") ||
                output.contains("abc123"));
    }

    @Test
    void testShowSuccessMessage() {
        notificationService.showSuccessMessage("Test success message");

        String output = outputStream.toString();
        boolean containsSuccessOrMessage = output.contains("SUCCESS") ||
                output.contains("✅") ||
                output.contains("Test success message");
        assertTrue(containsSuccessOrMessage,
                "Output should contain 'SUCCESS', '✅', or 'Test success message'. Actual output: " + output);
    }

    @Test
    void testShowInfoMessage() {
        notificationService.showInfoMessage("Test info message");

        String output = outputStream.toString();
        boolean containsInfoOrMessage = output.contains("INFO") ||
                output.contains("ℹ️") ||
                output.contains("Test info message");
        assertTrue(containsInfoOrMessage,
                "Output should contain 'INFO', 'ℹ️', or 'Test info message'. Actual output: " + output);
    }

    @Test
    void testNotifyLinksCleanup() {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        List<Link> links = Arrays.asList(
                createTestLink(user.getId(), "https://example1.com", "abc123",
                        now.minusHours(2), now.minusHours(1), 10, 5, false, "Link 1"),
                createTestLink(user.getId(), "https://example2.com", "def456",
                        now.minusHours(3), now.minusHours(2), 20, 10, false, "Link 2")
        );

        notificationService.notifyLinksCleanup(links);

        String output = outputStream.toString();
        assertFalse(output.trim().isEmpty(), "Cleanup should produce output");
    }

    @Test
    void testNotifyLinkExpired() {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        Link link = createTestLink(
                user.getId(),
                "https://example.com",
                "abc123",
                now.minusHours(2),
                now.minusHours(1),
                10,
                5,
                false,
                "Test link"
        );

        notificationService.notifyLinkExpired(user, link);

        String output = outputStream.toString();
        assertFalse(output.trim().isEmpty(), "Expired notification should produce output");
    }

    @Test
    void testNotifyLinkLimitReached() {
        User user = new User();
        user.setNotificationEmail("test@example.com");
        LocalDateTime now = LocalDateTime.now();

        Link link = createTestLink(
                user.getId(),
                "https://example.com",
                "abc123",
                now.minusHours(1),
                now.plusHours(24),
                10,
                10,
                false,
                "Test link"
        );

        notificationService.notifyLinkLimitReached(user, link);

        String output = outputStream.toString();
        assertFalse(output.trim().isEmpty(), "Limit reached notification should produce output");
    }

    @Test
    void testShowHelp() {
        notificationService.showHelp(24, 100);

        String output = outputStream.toString();
        assertTrue(output.length() > 100, "Help output should be substantial");
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
