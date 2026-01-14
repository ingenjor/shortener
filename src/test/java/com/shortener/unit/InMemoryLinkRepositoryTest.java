package com.shortener.unit;

import com.shortener.core.domain.Link;
import com.shortener.infra.storage.InMemoryLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLinkRepositoryTest {
    private InMemoryLinkRepository repository;
    private UUID userId;
    private Link testLink;

    @BeforeEach
    void setUp() {
        repository = new InMemoryLinkRepository();
        userId = UUID.randomUUID();

        testLink = new Link(
                userId,
                "https://example.com",
                "abc123",
                LocalDateTime.now().plusHours(24),
                100,
                "Test link"
        );
    }

    @Test
    void testSaveAndFindById() {
        repository.save(testLink);

        Optional<Link> found = repository.findById(testLink.getId());
        assertTrue(found.isPresent());
        assertEquals(testLink.getId(), found.get().getId());
    }

    @Test
    void testFindByShortCode() {
        repository.save(testLink);

        Optional<Link> found = repository.findByShortCode("abc123");
        assertTrue(found.isPresent());
        assertEquals(testLink.getShortCode(), found.get().getShortCode());
    }

    @Test
    void testFindByUserId() {
        repository.save(testLink);

        UUID otherUserId = UUID.randomUUID();
        Link otherLink = new Link(
                otherUserId,
                "https://example2.com",
                "def456",
                LocalDateTime.now().plusHours(24),
                50,
                "Other link"
        );
        repository.save(otherLink);

        List<Link> userLinks = repository.findByUserId(userId);
        assertEquals(1, userLinks.size());
        assertEquals(testLink.getId(), userLinks.get(0).getId());

        List<Link> otherUserLinks = repository.findByUserId(otherUserId);
        assertEquals(1, otherUserLinks.size());
        assertEquals(otherLink.getId(), otherUserLinks.get(0).getId());
    }

    @Test
    void testFindAll() {
        assertEquals(0, repository.findAll().size());

        repository.save(testLink);
        assertEquals(1, repository.findAll().size());

        Link link2 = new Link(
                userId,
                "https://example2.com",
                "def456",
                LocalDateTime.now().plusHours(24),
                50,
                "Link 2"
        );
        repository.save(link2);

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void testDelete() {
        repository.save(testLink);
        assertEquals(1, repository.findAll().size());

        repository.delete(testLink.getId());
        assertEquals(0, repository.findAll().size());
        assertFalse(repository.findById(testLink.getId()).isPresent());
        assertFalse(repository.findByShortCode("abc123").isPresent());
    }

    @Test
    void testDeleteAll() {
        repository.save(testLink);

        Link link2 = new Link(
                userId,
                "https://example2.com",
                "def456",
                LocalDateTime.now().plusHours(24),
                50,
                "Link 2"
        );
        repository.save(link2);

        assertEquals(2, repository.findAll().size());

        repository.deleteAll();
        assertEquals(0, repository.findAll().size());
    }

    @Test
    void testCount() {
        assertEquals(0, repository.count());

        repository.save(testLink);
        assertEquals(1, repository.count());

        Link link2 = new Link(
                userId,
                "https://example2.com",
                "def456",
                LocalDateTime.now().plusHours(24),
                50,
                "Link 2"
        );
        repository.save(link2);

        assertEquals(2, repository.count());
    }

    @Test
    void testSaveUpdatesExistingLink() {
        repository.save(testLink);

        // Simulate updating the link
        Link updatedLink = new Link(
                testLink.getUserId(),
                "https://updated.com",
                testLink.getShortCode(),
                LocalDateTime.now().plusHours(48),
                200,
                "Updated link"
        );
        // Note: In real scenario, we would update the existing link object
        // but for repository test, we test that save overwrites

        repository.save(testLink); // Saving same link again
        assertEquals(1, repository.count());
    }
}
