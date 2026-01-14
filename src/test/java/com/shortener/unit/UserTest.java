package com.shortener.unit;

import com.shortener.core.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserCreation() {
        User user = new User();

        assertNotNull(user.getId());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getLastActivity());
        assertTrue(user.getLinkIds().isEmpty());
        assertNull(user.getNotificationEmail());
    }

    @Test
    void testUserCreationWithUUID() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId);

        assertEquals(userId, user.getId());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getLastActivity());
        assertTrue(user.getLinkIds().isEmpty());
        assertNull(user.getNotificationEmail());
    }

    @Test
    void testAddLink() {
        User user = new User();
        UUID linkId = UUID.randomUUID();

        user.addLink(linkId);

        assertEquals(1, user.getLinkIds().size());
        assertTrue(user.getLinkIds().contains(linkId));
        assertTrue(user.ownsLink(linkId));
    }

    @Test
    void testRemoveLink() {
        User user = new User();
        UUID linkId = UUID.randomUUID();

        user.addLink(linkId);
        assertEquals(1, user.getLinkIds().size());

        user.removeLink(linkId);
        assertEquals(0, user.getLinkIds().size());
        assertFalse(user.ownsLink(linkId));
    }

    @Test
    void testRemoveNonExistentLink() {
        User user = new User();
        UUID linkId = UUID.randomUUID();

        user.removeLink(linkId); // Should not throw
        assertEquals(0, user.getLinkIds().size());
    }

    @Test
    void testUpdateActivity() {
        User user = new User();
        LocalDateTime initialActivity = user.getLastActivity();

        // Wait a bit
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }

        user.updateActivity();

        assertTrue(user.getLastActivity().isAfter(initialActivity));
    }

    @Test
    void testSetNotificationEmail() {
        User user = new User();

        user.setNotificationEmail("test@example.com");
        assertEquals("test@example.com", user.getNotificationEmail());

        user.setNotificationEmail("another@test.com");
        assertEquals("another@test.com", user.getNotificationEmail());
    }

    @Test
    void testSetNotificationEmailToNull() {
        User user = new User();

        user.setNotificationEmail("test@example.com");
        assertNotNull(user.getNotificationEmail());

        user.setNotificationEmail(null);
        assertNull(user.getNotificationEmail());
    }

    @Test
    void testToString() {
        User user = new User();
        String str = user.toString();

        assertNotNull(str);
        assertTrue(str.contains("User{"));
        assertTrue(str.contains("id=" + user.getId()));
    }

    @Test
    void testMultipleLinks() {
        User user = new User();
        UUID linkId1 = UUID.randomUUID();
        UUID linkId2 = UUID.randomUUID();
        UUID linkId3 = UUID.randomUUID();

        user.addLink(linkId1);
        user.addLink(linkId2);
        user.addLink(linkId3);

        assertEquals(3, user.getLinkIds().size());
        assertTrue(user.ownsLink(linkId1));
        assertTrue(user.ownsLink(linkId2));
        assertTrue(user.ownsLink(linkId3));

        user.removeLink(linkId2);
        assertEquals(2, user.getLinkIds().size());
        assertTrue(user.ownsLink(linkId1));
        assertFalse(user.ownsLink(linkId2));
        assertTrue(user.ownsLink(linkId3));
    }

    @Test
    void testUnmodifiableLinkIds() {
        User user = new User();

        // Should not be able to modify the returned set
        assertThrows(UnsupportedOperationException.class, () -> {
            user.getLinkIds().add(UUID.randomUUID());
        });
    }
}
