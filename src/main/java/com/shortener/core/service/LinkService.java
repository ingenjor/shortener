package com.shortener.core.service;

import com.shortener.core.domain.Link;
import com.shortener.core.domain.User;
import com.shortener.core.exception.LinkExpiredException;
import com.shortener.core.exception.LinkNotFoundException;
import com.shortener.core.exception.AccessDeniedException;
import com.shortener.core.repository.LinkRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class LinkService {
    private final LinkRepository linkRepository;
    private final ShortCodeGenerator codeGenerator;
    private final int defaultTtlHours;
    private final int defaultMaxClicks;

    public LinkService(LinkRepository linkRepository, ShortCodeGenerator codeGenerator,
                       int defaultTtlHours, int defaultMaxClicks) {
        this.linkRepository = linkRepository;
        this.codeGenerator = codeGenerator;
        this.defaultTtlHours = defaultTtlHours;
        this.defaultMaxClicks = defaultMaxClicks;
    }

    // Время жизни ТОЛЬКО из конфигурации
    public Link createLink(User user, String originalUrl, Integer maxClicks, String description) {
        // Генерация уникального кода для пользователя
        String shortCode = codeGenerator.generateCode(originalUrl, user.getId());

        // Время жизни ТОЛЬКО из конфигурации
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(defaultTtlHours);

        // Установка лимита переходов (может быть задан пользователем)
        int actualMaxClicks = maxClicks != null ? maxClicks : defaultMaxClicks;

        // Создание ссылки
        Link link = new Link(
                user.getId(),
                originalUrl,
                shortCode,
                expiresAt,
                actualMaxClicks,
                description
        );

        // Сохранение
        linkRepository.save(link);

        return link;
    }

    public String getOriginalUrl(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException("Link not found: " + shortCode));

        if (!link.canBeAccessed()) {
            if (link.isExpired()) {
                throw new LinkExpiredException("Link has expired");
            } else if (link.hasReachedLimit()) {
                throw new IllegalStateException("Click limit reached");
            } else {
                throw new IllegalStateException("Link is not active");
            }
        }

        link.incrementClicks();
        linkRepository.save(link);

        return link.getOriginalUrl();
    }

    public Link getLink(String shortCode, UUID userId) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException("Link not found: " + shortCode));

        if (!link.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied to link: " + shortCode);
        }

        return link;
    }

    public List<Link> getUserLinks(UUID userId) {
        return linkRepository.findByUserId(userId);
    }

    public Link updateMaxClicks(String shortCode, UUID userId, int newMaxClicks) {
        Link link = getLink(shortCode, userId);
        link.updateMaxClicks(newMaxClicks);
        linkRepository.save(link);
        return link;
    }

    // УДАЛЕН метод extendExpiration - время жизни ТОЛЬКО из конфигурации
    // public Link extendExpiration(String shortCode, UUID userId, int additionalHours) { ... }

    public void deactivateLink(String shortCode, UUID userId) {
        Link link = getLink(shortCode, userId);
        link.deactivate();
        linkRepository.save(link);
    }

    public void deleteLink(String shortCode, UUID userId) {
        Link link = getLink(shortCode, userId);
        linkRepository.delete(link.getId());
    }

    public List<Link> findExpiredLinks() {
        return linkRepository.findAll().stream()
                .filter(Link::isExpired)
                .collect(Collectors.toList());
    }

    public List<Link> findLinksReachingLimit(int thresholdPercent) {
        return linkRepository.findAll().stream()
                .filter(link -> {
                    if (link.getMaxClicks() == 0) return false;
                    double percentage = (link.getCurrentClicks() * 100.0) / link.getMaxClicks();
                    return percentage >= thresholdPercent && link.canBeAccessed();
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getStatistics(String shortCode, UUID userId) {
        Link link = getLink(shortCode, userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("shortCode", link.getShortCode());
        stats.put("originalUrl", link.getOriginalUrl());
        stats.put("createdAt", link.getCreatedAt());
        stats.put("expiresAt", link.getExpiresAt());
        stats.put("maxClicks", link.getMaxClicks());
        stats.put("currentClicks", link.getCurrentClicks());
        stats.put("isActive", link.isActive());
        stats.put("isExpired", link.isExpired());
        stats.put("hasReachedLimit", link.hasReachedLimit());
        stats.put("canBeAccessed", link.canBeAccessed());
        stats.put("description", link.getDescription());

        long hoursLeft = java.time.Duration.between(
                LocalDateTime.now(), link.getExpiresAt()
        ).toHours();
        stats.put("hoursLeft", Math.max(0, hoursLeft));

        double usagePercentage = link.getMaxClicks() > 0 ?
                (link.getCurrentClicks() * 100.0) / link.getMaxClicks() : 0;
        stats.put("usagePercentage", Math.min(100.0, usagePercentage));

        return stats;
    }
}
