package com.shortener.unit;

import com.shortener.core.domain.ClickStatistic;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClickStatisticTest {

    @Test
    void testClickStatisticCreation() {
        UUID linkId = UUID.randomUUID();
        String userAgent = "Mozilla/5.0";
        String ipAddress = "192.168.1.1";

        ClickStatistic click = new ClickStatistic(linkId, userAgent, ipAddress);

        assertEquals(linkId, click.getLinkId());
        assertEquals(userAgent, click.getUserAgent());
        assertEquals(ipAddress, click.getIpAddress());
        assertNotNull(click.getClickedAt());

        // ClickedAt should be recent
        assertTrue(click.getClickedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(click.getClickedAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void testClickStatisticWithNullValues() {
        UUID linkId = UUID.randomUUID();

        ClickStatistic click = new ClickStatistic(linkId, null, null);

        assertEquals(linkId, click.getLinkId());
        assertNull(click.getUserAgent());
        assertNull(click.getIpAddress());
    }

    @Test
    void testClickStatisticWithEmptyStrings() {
        UUID linkId = UUID.randomUUID();

        ClickStatistic click = new ClickStatistic(linkId, "", "");

        assertEquals(linkId, click.getLinkId());
        assertEquals("", click.getUserAgent());
        assertEquals("", click.getIpAddress());
    }

    @Test
    void testMultipleClickStatistics() {
        UUID linkId1 = UUID.randomUUID();
        UUID linkId2 = UUID.randomUUID();

        ClickStatistic click1 = new ClickStatistic(linkId1, "Agent1", "192.168.1.1");
        ClickStatistic click2 = new ClickStatistic(linkId2, "Agent2", "192.168.1.2");

        assertNotEquals(click1.getLinkId(), click2.getLinkId());
        assertNotEquals(click1.getUserAgent(), click2.getUserAgent());
        assertNotEquals(click1.getIpAddress(), click2.getIpAddress());
    }
}
