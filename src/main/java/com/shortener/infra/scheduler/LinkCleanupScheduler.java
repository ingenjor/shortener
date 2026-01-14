package com.shortener.infra.scheduler;

import com.shortener.core.domain.Link;
import com.shortener.core.repository.LinkRepository;
import com.shortener.core.service.NotificationService;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LinkCleanupScheduler {
    private final LinkRepository linkRepository;
    private final NotificationService notificationService;
    private final boolean autoDeleteExpired;
    private final ScheduledExecutorService scheduler;
    private final int checkIntervalMinutes;

    public LinkCleanupScheduler(LinkRepository linkRepository,
                                NotificationService notificationService,
                                boolean autoDeleteExpired,
                                int checkIntervalMinutes) {
        this.linkRepository = linkRepository;
        this.notificationService = notificationService;
        this.autoDeleteExpired = autoDeleteExpired;
        this.checkIntervalMinutes = checkIntervalMinutes;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::cleanupExpiredLinks,
                0,
                checkIntervalMinutes,
                TimeUnit.MINUTES
        );

        System.out.println("🔧 Link cleanup scheduler started (interval: " +
                checkIntervalMinutes + " minutes)");
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void cleanupExpiredLinks() {
        List<Link> expiredLinks = linkRepository.findAll().stream()
                .filter(Link::isExpired)
                .collect(Collectors.toList());

        if (!expiredLinks.isEmpty()) {
            if (autoDeleteExpired) {
                expiredLinks.forEach(link -> linkRepository.delete(link.getId()));
                notificationService.notifyLinksCleanup(expiredLinks);
            } else {
                // Только помечаем как неактивные
                expiredLinks.forEach(link -> {
                    if (link.isActive()) {
                        link.deactivate();
                        linkRepository.save(link);
                    }
                });
            }
        }
    }
}
