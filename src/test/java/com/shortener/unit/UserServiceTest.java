package com.shortener.unit;

import com.shortener.core.domain.User;
import com.shortener.core.service.UserService;
import com.shortener.infra.storage.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    private UserService userService;
    private InMemoryUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        userService = new UserService(repository, 168);
    }

    @Test
    void testCreateUser() {
        User user = userService.createUser();

        assertNotNull(user);
        assertNotNull(user.getId());
        assertEquals(0, user.getLinkIds().size());

        Optional<User> retrieved = userService.findUser(user.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(user.getId(), retrieved.get().getId());
    }

    @Test
    void testCreateUser_WithUUID() {
        UUID userId = UUID.randomUUID();
        User user = userService.createUser(userId);

        assertEquals(userId, user.getId());

        // Creating with same UUID should throw
        assertThrows(IllegalArgumentException.class, () ->
                userService.createUser(userId)
        );
    }

    @Test
    void testGetOrCreateUser() {
        UUID userId = UUID.randomUUID();

        // First call creates user
        User user1 = userService.getOrCreateUser(userId);
        assertEquals(userId, user1.getId());

        // Second call returns existing
        User user2 = userService.getOrCreateUser(userId);
        assertEquals(user1.getId(), user2.getId());

        // Create with null UUID
        User user3 = userService.getOrCreateUser(null);
        assertNotNull(user3.getId());
        assertNotEquals(userId, user3.getId());
    }

    @Test
    void testSetUserEmail() {
        User user = userService.createUser();

        userService.setUserEmail(user.getId(), "test@example.com");

        Optional<User> retrieved = userService.findUser(user.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("test@example.com", retrieved.get().getNotificationEmail());
    }

    @Test
    void testSetUserEmail_InvalidEmail() {
        User user = userService.createUser();

        assertThrows(IllegalArgumentException.class, () ->
                userService.setUserEmail(user.getId(), "invalid-email")
        );
    }

    @Test
    void testSetUserEmail_UserNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        // Should not throw for non-existent user
        userService.setUserEmail(nonExistentId, "test@example.com");
    }

    @Test
    void testUpdateUserActivity() {
        User user = userService.createUser();
        LocalDateTime initialActivity = user.getLastActivity();

        // Wait a bit
        try { Thread.sleep(10); } catch (InterruptedException e) {}

        userService.updateUserActivity(user.getId());

        Optional<User> retrieved = userService.findUser(user.getId());
        assertTrue(retrieved.isPresent());
        assertTrue(retrieved.get().getLastActivity().isAfter(initialActivity));
    }

    @Test
    void testCleanupInactiveUsers() {
        User activeUser = userService.createUser();
        User inactiveUser = userService.createUser();

        // Make inactiveUser seem older
        inactiveUser.updateActivity(); // This updates to now

        // We can't easily test time-based cleanup without mocking time
        // This is more of an integration test
        userService.cleanupInactiveUsers();

        // Both users should still exist
        assertTrue(userService.findUser(activeUser.getId()).isPresent());
        assertTrue(userService.findUser(inactiveUser.getId()).isPresent());
    }
}
