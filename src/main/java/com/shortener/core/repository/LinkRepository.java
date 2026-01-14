package com.shortener.core.repository;

import com.shortener.core.domain.Link;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository {
    Optional<Link> findById(UUID id);
    Optional<Link> findByShortCode(String shortCode);
    List<Link> findByUserId(UUID userId);
    List<Link> findAll();
    Link save(Link link);
    void delete(UUID id);
    void deleteAll();
    long count();
}
