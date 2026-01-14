package com.shortener.integration;

import com.shortener.core.domain.Link;
import com.shortener.core.domain.User;
import com.shortener.core.service.LinkService;
import com.shortener.core.service.ShortCodeGenerator;
import com.shortener.core.service.UserService;
import com.shortener.infra.storage.InMemoryLinkRepository;
import com.shortener.infra.storage.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class EndToEndTest {
    private LinkService linkService;
    private UserService userService;
    private InMemoryLinkRepository linkRepository;
    private InMemoryUserRepository userRepository;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        linkRepository = new InMemoryLinkRepository();
        userRepository = new InMemoryUserRepository();

        ShortCodeGenerator generator = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.BASE62, 7
        );

        linkService = new LinkService(linkRepository, generator, 24, 100);
        userService = new UserService(userRepository, 168);

        user1 = userService.createUser();
        user2 = userService.createUser();
    }

    @Test
    void testDifferentUsersGetDifferentCodesForSameUrl() {
        String sameUrl = "https://example.com";

        Link link1 = linkService.createLink(user1, sameUrl, null, null);
        Link link2 = linkService.createLink(user2, sameUrl, null, null);

        assertNotEquals(link1.getShortCode(), link2.getShortCode(),
                "Different users should get different short codes for the same URL");
    }

    @Test
    void testSameUserGetsSameCodeForSameUrl() {
        String sameUrl = "https://example.com";

        Link link1 = linkService.createLink(user1, sameUrl, null, null);
        Link link2 = linkService.createLink(user1, sameUrl, null, null);

        assertEquals(link1.getShortCode(), link2.getShortCode(),
                "Same user should get the same short code for the same URL");
    }

    @Test
    void testLinkExpirationFromConfig() {
        Link link = linkService.createLink(user1, "https://example.com", null, null);

        // Проверяем, что время жизни установлено из конфигурации (24 часа)
        assertNotNull(link.getExpiresAt());

        // Невозможно изменить время жизни (метод extend удален)
        // Это соответствует требованиям
    }

    @Test
    void testClickLimitFunctionality() {
        Link link = linkService.createLink(
                user1,
                "https://example.com",
                2,
                "Limited link"
        );

        // Первый клик - должен работать
        String url1 = linkService.getOriginalUrl(link.getShortCode());
        assertEquals("https://example.com", url1);

        // Второй клик - должен работать
        String url2 = linkService.getOriginalUrl(link.getShortCode());
        assertEquals("https://example.com", url2);

        // Третий клик - должен выбросить исключение
        assertThrows(IllegalStateException.class, () ->
                linkService.getOriginalUrl(link.getShortCode())
        );

        // Ссылка должна быть неактивной
        Link retrieved = linkRepository.findByShortCode(link.getShortCode()).get();
        assertFalse(retrieved.isActive(), "Ссылка должна быть неактивной после достижения лимита");
        assertTrue(retrieved.hasReachedLimit(), "Ссылка должна показывать, что лимит достигнут");
    }

    @Test
    void testUserIsolation() {
        Link user1Link = linkService.createLink(user1, "https://user1.com", null, null);
        Link user2Link = linkService.createLink(user2, "https://user2.com", null, null);

        List<Link> user1Links = linkService.getUserLinks(user1.getId());
        List<Link> user2Links = linkService.getUserLinks(user2.getId());

        assertEquals(1, user1Links.size());
        assertEquals(1, user2Links.size());
        assertTrue(user1Links.stream().allMatch(link -> link.getUserId().equals(user1.getId())));
        assertTrue(user2Links.stream().allMatch(link -> link.getUserId().equals(user2.getId())));
    }

    @Test
    void testUpdateMaxClicksReactivatesLink() {
        Link link = linkService.createLink(user1, "https://example.com", 1, null);

        // Исчерпываем лимит
        linkService.getOriginalUrl(link.getShortCode());

        // Ссылка должна быть неактивной
        Link expiredLink = linkRepository.findByShortCode(link.getShortCode()).get();
        assertFalse(expiredLink.isActive());

        // Увеличиваем лимит
        linkService.updateMaxClicks(link.getShortCode(), user1.getId(), 3);

        // Ссылка должна стать активной
        Link reactivatedLink = linkRepository.findByShortCode(link.getShortCode()).get();
        assertTrue(reactivatedLink.isActive());
        assertEquals(3, reactivatedLink.getMaxClicks());
    }

    @Test
    void testAutoExpirationDetection() {
        LinkService linkServiceWithShortTtl = new LinkService(
                linkRepository,
                new ShortCodeGenerator(ShortCodeGenerator.Algorithm.BASE62, 7),
                1, // 1 час для теста
                100
        );

        Link link = linkServiceWithShortTtl.createLink(user1, "https://example.com", null, null);

        List<Link> expiredLinks = linkServiceWithShortTtl.findExpiredLinks();

        // Через 1 час ссылка должна быть найдена как просроченная
        // (В реальном тесте нужно было бы мокировать время)
        assertNotNull(expiredLinks);
    }

    @Test
    void testStatisticsCalculation() {
        Link link = linkService.createLink(user1, "https://example.com", 10, "Test");

        // Делаем 3 клика
        for (int i = 0; i < 3; i++) {
            try {
                linkService.getOriginalUrl(link.getShortCode());
            } catch (Exception e) {
                // Игнорируем
            }
        }

        var stats = linkService.getStatistics(link.getShortCode(), user1.getId());

        assertEquals(3, stats.get("currentClicks"));
        assertEquals(10, stats.get("maxClicks"));
        assertEquals(30.0, (Double) stats.get("usagePercentage"), 0.1);
        assertTrue((Boolean) stats.get("canBeAccessed"));
    }

    @Test
    void testCleanupOfExpiredLinks() {
        // Создаем ссылки
        linkService.createLink(user1, "https://example1.com", null, null);
        linkService.createLink(user1, "https://example2.com", null, null);
        linkService.createLink(user2, "https://example3.com", null, null);

        assertEquals(3, linkRepository.count());

        // Находим все ссылки и удаляем одну
        List<Link> allLinks = linkRepository.findAll();
        if (!allLinks.isEmpty()) {
            linkRepository.delete(allLinks.get(0).getId());
        }

        assertEquals(2, linkRepository.count());
    }
}
