package com.shortener.core.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class ClickStatistic {
    private final UUID linkId;
    private final LocalDateTime clickedAt;
    private final String userAgent;
    private final String ipAddress;

    public ClickStatistic(UUID linkId, String userAgent, String ipAddress) {
        this.linkId = linkId;
        this.clickedAt = LocalDateTime.now();
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    // Getters
    public UUID getLinkId() { return linkId; }
    public LocalDateTime getClickedAt() { return clickedAt; }
    public String getUserAgent() { return userAgent; }
    public String getIpAddress() { return ipAddress; }
}
