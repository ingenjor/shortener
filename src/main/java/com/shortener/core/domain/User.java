package com.shortener.core.domain;

import java.time.LocalDateTime;
import java.util.*;

public class User {
    private final UUID id;
    private final LocalDateTime createdAt;
    private final Set<UUID> linkIds;
    private String notificationEmail;
    private LocalDateTime lastActivity;

    public User() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.linkIds = new HashSet<>();
        this.lastActivity = LocalDateTime.now();
    }

    public User(UUID id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.linkIds = new HashSet<>();
        this.lastActivity = LocalDateTime.now();
    }

    public void addLink(UUID linkId) {
        linkIds.add(linkId);
        updateActivity();
    }

    public void removeLink(UUID linkId) {
        linkIds.remove(linkId);
        updateActivity();
    }

    public boolean ownsLink(UUID linkId) {
        return linkIds.contains(linkId);
    }

    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    // Getters
    public UUID getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Set<UUID> getLinkIds() { return Collections.unmodifiableSet(linkIds); }
    public String getNotificationEmail() { return notificationEmail; }
    public LocalDateTime getLastActivity() { return lastActivity; }

    // Setters
    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", linkCount=" + linkIds.size() +
                ", lastActivity=" + lastActivity +
                '}';
    }
}
