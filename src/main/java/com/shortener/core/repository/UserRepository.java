package com.shortener.core.repository;

import com.shortener.core.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);
    List<User> findAll();
    User save(User user);
    void delete(UUID id);
    void deleteAll();
    long count();
}
