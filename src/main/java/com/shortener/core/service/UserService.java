package com.shortener.core.service;

import com.shortener.core.domain.User;
import com.shortener.core.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class UserService {
    private final UserRepository userRepository;
    private final int userSessionTtlHours;

    public UserService(UserRepository userRepository, int userSessionTtlHours) {
        this.userRepository = userRepository;
        this.userSessionTtlHours = userSessionTtlHours;
    }

    public User getOrCreateUser(UUID userId) {
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElseGet(() -> createUser(userId));
        }
        return createUser();
    }

    public User createUser() {
        User user = new User();
        userRepository.save(user);
        return user;
    }

    public User createUser(UUID userId) {
        // Проверяем, не существует ли уже пользователь с таким ID
        if (userRepository.findById(userId).isPresent()) {
            throw new IllegalArgumentException("User with this UUID already exists");
        }
        User user = new User(userId);
        userRepository.save(user);
        return user;
    }

    public Optional<User> findUser(UUID userId) {
        return userRepository.findById(userId);
    }

    public void updateUserActivity(UUID userId) {
        userRepository.findById(userId).ifPresent(User::updateActivity);
    }

    public void cleanupInactiveUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(userSessionTtlHours);
        userRepository.findAll().stream()
                .filter(user -> user.getLastActivity().isBefore(cutoff))
                .forEach(user -> userRepository.delete(user.getId()));
    }

    public void setUserEmail(UUID userId, String email) {
        userRepository.findById(userId).ifPresent(user -> {
            if (email != null && !email.isEmpty()) {
                // Простая валидация email
                if (email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    user.setNotificationEmail(email);
                    userRepository.save(user);
                } else {
                    throw new IllegalArgumentException("Invalid email format");
                }
            }
        });
    }
}
