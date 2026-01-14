package com.shortener.unit;

import com.shortener.core.domain.User;
import com.shortener.infra.storage.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserRepositoryTest {
    private InMemoryUserRepository repository;
    private User testUser;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        testUser = new User();
    }

    @Test
    void testSaveAndFindById() {
        repository.save(testUser);

        Optional<User> found = repository.findById(testUser.getId());
        assertTrue(found.isPresent());
        assertEquals(testUser.getId(), found.get().getId());
    }

    @Test
    void testFindAll() {
        assertEquals(0, repository.findAll().size());

        repository.save(testUser);
        assertEquals(1, repository.findAll().size());

        User user2 = new User();
        repository.save(user2);

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void testDelete() {
        repository.save(testUser);
        assertEquals(1, repository.findAll().size());

        repository.delete(testUser.getId());
        assertEquals(0, repository.findAll().size());
        assertFalse(repository.findById(testUser.getId()).isPresent());
    }

    @Test
    void testDeleteAll() {
        repository.save(testUser);

        User user2 = new User();
        repository.save(user2);

        assertEquals(2, repository.findAll().size());

        repository.deleteAll();
        assertEquals(0, repository.findAll().size());
    }

    @Test
    void testCount() {
        assertEquals(0, repository.count());

        repository.save(testUser);
        assertEquals(1, repository.count());

        User user2 = new User();
        repository.save(user2);

        assertEquals(2, repository.count());
    }

    @Test
    void testSaveUpdatesExistingUser() {
        repository.save(testUser);

        // Update user email
        testUser.setNotificationEmail("test@example.com");
        repository.save(testUser);

        Optional<User> found = repository.findById(testUser.getId());
        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getNotificationEmail());
    }

    @Test
    void testFindByIdNonExistent() {
        UUID nonExistentId = UUID.randomUUID();
        Optional<User> found = repository.findById(nonExistentId);
        assertFalse(found.isPresent());
    }

    @Test
    void testMultipleOperations() {
        // Test sequence of operations
        assertEquals(0, repository.count());

        User user1 = new User();
        repository.save(user1);
        assertEquals(1, repository.count());

        User user2 = new User();
        repository.save(user2);
        assertEquals(2, repository.count());

        repository.delete(user1.getId());
        assertEquals(1, repository.count());
        assertFalse(repository.findById(user1.getId()).isPresent());
        assertTrue(repository.findById(user2.getId()).isPresent());

        repository.deleteAll();
        assertEquals(0, repository.count());
    }
}
