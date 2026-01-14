package com.shortener.unit;

import com.shortener.core.domain.Link;
import com.shortener.core.domain.User;
import com.shortener.core.exception.AccessDeniedException;
import com.shortener.core.exception.LinkNotFoundException;
import com.shortener.core.service.LinkService;
import com.shortener.core.service.ShortCodeGenerator;
import com.shortener.infra.storage.InMemoryLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkServiceTest {
    private LinkService linkService;
    private User testUser;
    private User otherUser;
    private InMemoryLinkRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryLinkRepository();
        ShortCodeGenerator generator = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.RANDOM, 7
        );
        linkService = new LinkService(repository, generator, 24, 100);
        testUser = new User();
        otherUser = new User();
    }

    @Test
    void testCreateLink_UsesDefaultTtlFromConfig() {
        Link link = linkService.createLink(
                testUser,
                "https://example.com",
                null,
                null
        );

        LocalDateTime expectedExpiration = LocalDateTime.now().plusHours(24);
        assertTrue(
                link.getExpiresAt().isAfter(LocalDateTime.now().plusHours(23)) &&
                        link.getExpiresAt().isBefore(LocalDateTime.now().plusHours(25)),
                "Expiration should be approximately 24 hours from creation"
        );
    }

    @Test
    void testCreateLink_UsesUserProvidedMaxClicks() {
        Link link = linkService.createLink(
                testUser,
                "https://example.com",
                50,
                null
        );

        assertEquals(50, link.getMaxClicks());
    }

    @Test
    void testGetOriginalUrl_ThrowsWhenLimitReached() {
        Link link = linkService.createLink(
                testUser,
                "https://example.com",
                1, // Лимит: 1 клик
                null
        );

        // Первый клик должен работать
        linkService.getOriginalUrl(link.getShortCode());

        // Второй клик должен выбросить исключение
        assertThrows(IllegalStateException.class, () ->
                linkService.getOriginalUrl(link.getShortCode())
        );

        // Ссылка должна быть НЕактивной после достижения лимита
        Link updatedLink = repository.findByShortCode(link.getShortCode()).get();
        assertFalse(updatedLink.isActive(), "Ссылка должна быть неактивной после достижения лимита");
    }

    @Test
    void testGetOriginalUrl_ThrowsWhenExpired() {
        // Вместо создания ссылки с коротким TTL, создадим истекшую ссылку напрямую
        LocalDateTime now = LocalDateTime.now();
        Link expiredLink = createTestLink(
                testUser.getId(),
                "https://expired.com",
                "expired123",
                now.minusHours(2), // создана 2 часа назад
                now.minusHours(1), // истекла 1 час назад
                10,
                0,
                true,
                "Expired"
        );

        repository.save(expiredLink);

        // Должен выбросить исключение при попытке доступа
        assertThrows(Exception.class, () ->
                linkService.getOriginalUrl(expiredLink.getShortCode())
        );
    }

    @Test
    void testGetLink_AccessDeniedForOtherUser() {
        Link link = linkService.createLink(
                testUser,
                "https://example.com",
                null,
                null
        );

        // Владелец может получить ссылку
        assertDoesNotThrow(() ->
                linkService.getLink(link.getShortCode(), testUser.getId())
        );

        // Другой пользователь не может
        assertThrows(AccessDeniedException.class, () ->
                linkService.getLink(link.getShortCode(), otherUser.getId())
        );
    }

    @Test
    void testUpdateMaxClicks() {
        Link link = linkService.createLink(
                testUser,
                "https://example.com",
                10,
                null
        );

        // Изменяем лимит
        Link updated = linkService.updateMaxClicks(
                link.getShortCode(),
                testUser.getId(),
                20
        );

        assertEquals(20, updated.getMaxClicks());
    }

    @Test
    void testUpdateMaxClicks_CannotSetBelowCurrentClicks() {
        Link link = linkService.createLink(
                testUser,
                "https://example.com",
                10,
                null
        );

        // Делаем 5 кликов
        for (int i = 0; i < 5; i++) {
            try {
                linkService.getOriginalUrl(link.getShortCode());
            } catch (Exception e) {
                // Игнорируем для теста
            }
        }

        // Нельзя установить лимит ниже текущих кликов
        assertThrows(IllegalArgumentException.class, () ->
                linkService.updateMaxClicks(link.getShortCode(), testUser.getId(), 3)
        );
    }

    @Test
    void testDeleteLink() {
        Link link = linkService.createLink(
                testUser,
                "https://example.com",
                null,
                null
        );

        assertEquals(1, repository.count());

        linkService.deleteLink(link.getShortCode(), testUser.getId());

        assertEquals(0, repository.count());
        assertThrows(LinkNotFoundException.class, () ->
                linkService.getLink(link.getShortCode(), testUser.getId())
        );
    }

    @Test
    void testGetUserLinks() {
        // Создаем несколько ссылок для пользователя
        linkService.createLink(testUser, "https://example1.com", null, null);
        linkService.createLink(testUser, "https://example2.com", null, null);
        linkService.createLink(otherUser, "https://example3.com", null, null);

        List<Link> userLinks = linkService.getUserLinks(testUser.getId());

        assertEquals(2, userLinks.size());
        assertTrue(userLinks.stream().allMatch(link ->
                link.getUserId().equals(testUser.getId())
        ));
    }

    @Test
    void testFindExpiredLinks() {
        LocalDateTime now = LocalDateTime.now();

        // Создаем истекшую ссылку напрямую
        Link expiredLink = createTestLink(
                testUser.getId(),
                "https://expired.com",
                "expired123",
                now.minusHours(2),
                now.minusHours(1),
                10,
                0,
                true,
                "Expired"
        );

        // Создаем активную ссылку через сервис
        Link activeLink = linkService.createLink(
                testUser,
                "https://active.com",
                null,
                null
        );

        repository.save(expiredLink);

        List<Link> expiredLinks = linkService.findExpiredLinks();

        // Должна найти истекшую ссылку
        assertTrue(expiredLinks.stream()
                .anyMatch(link -> link.getShortCode().equals("expired123")));
    }

    @Test
    void testFindLinksReachingLimit() {
        Link link = linkService.createLink(
                testUser,
                "https://example.com",
                10,
                null
        );

        // Делаем 8 кликов (80%)
        for (int i = 0; i < 8; i++) {
            try {
                linkService.getOriginalUrl(link.getShortCode());
            } catch (Exception e) {
                // Игнорируем для теста
            }
        }

        List<Link> nearLimitLinks = linkService.findLinksReachingLimit(75);

        assertEquals(1, nearLimitLinks.size());
        assertEquals(link.getShortCode(), nearLimitLinks.get(0).getShortCode());
    }

    @Test
    void testGetStatistics() {
        Link link = linkService.createLink(
                testUser,
                "https://example.com",
                100,
                "Test description"
        );

        var stats = linkService.getStatistics(link.getShortCode(), testUser.getId());

        assertEquals(link.getShortCode(), stats.get("shortCode"));
        assertEquals("https://example.com", stats.get("originalUrl"));
        assertEquals(100, stats.get("maxClicks"));
        assertEquals(0, stats.get("currentClicks"));
        assertEquals("Test description", stats.get("description"));
        assertTrue((Boolean) stats.get("canBeAccessed"));
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
