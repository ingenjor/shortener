package com.shortener.core.domain;

import org.apache.commons.validator.routines.UrlValidator;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Доменный объект, представляющий сокращенную ссылку.
 * Включает все параметры: лимит переходов, время жизни, уникальность для пользователя.
 * Важно: время жизни (expiresAt) задается только при создании и не может быть изменено .
 */
public class Link {
    private static final UrlValidator URL_VALIDATOR = new UrlValidator(
            new String[]{"http", "https", "ftp"}
    );

    private final UUID id;
    private final UUID userId;
    private final String originalUrl;
    private final String shortCode;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt; // final - время жизни нельзя изменить после создания
    private int maxClicks;
    private int currentClicks;
    private boolean isActive;
    private final String description;

    /**
     * Создает новую ссылку.
     * Время жизни устанавливается из конфигурации и не может быть изменено пользователем .
     *
     * @param userId UUID пользователя-владельца
     * @param originalUrl оригинальный URL для сокращения
     * @param shortCode короткий код ссылки
     * @param expiresAt время истечения срока действия (из конфигурации)
     * @param maxClicks максимальное количество переходов (может быть задано пользователем)
     * @param description описание ссылки (опционально)
     * @throws IllegalArgumentException если URL невалиден или параметры некорректны
     */
    public Link(UUID userId, String originalUrl, String shortCode,
                LocalDateTime expiresAt, int maxClicks, String description) {
        this.id = UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "UserId cannot be null");
        this.originalUrl = validateUrl(originalUrl);
        this.shortCode = Objects.requireNonNull(shortCode, "ShortCode cannot be null");
        this.createdAt = LocalDateTime.now();
        this.expiresAt = Objects.requireNonNull(expiresAt, "ExpiresAt cannot be null");
        this.maxClicks = maxClicks > 0 ? maxClicks : 1;
        this.currentClicks = 0;
        this.isActive = true;
        this.description = description != null ? description : "";

        validateState();
    }

    /**
     * Конструктор для тестирования, который позволяет установить createdAt и expiresAt
     * Используется ТОЛЬКО в тестах
     */
    protected Link(UUID userId, String originalUrl, String shortCode,
                   LocalDateTime createdAt, LocalDateTime expiresAt,
                   int maxClicks, int currentClicks, boolean isActive, String description) {
        this.id = UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "UserId cannot be null");
        this.originalUrl = validateUrl(originalUrl);
        this.shortCode = Objects.requireNonNull(shortCode, "ShortCode cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "ExpiresAt cannot be null");
        this.maxClicks = maxClicks > 0 ? maxClicks : 1;
        this.currentClicks = Math.max(0, currentClicks);
        this.isActive = isActive;
        this.description = description != null ? description : "";

        // Для тестов не проверяем, что expiresAt после createdAt
    }

    /**
     * Проверяет валидность URL.
     * Использует Apache Commons Validator для строгой проверки.
     *
     * @param url URL для проверки
     * @return валидный URL
     * @throws IllegalArgumentException если URL невалиден
     */
    private String validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        String trimmedUrl = url.trim();

        if (!URL_VALIDATOR.isValid(trimmedUrl)) {
            throw new IllegalArgumentException(
                    String.format("Invalid URL format: '%s'. URL must start with http://, https:// or ftp://",
                            trimmedUrl.length() > 50 ? trimmedUrl.substring(0, 47) + "..." : trimmedUrl)
            );
        }

        if (trimmedUrl.length() > 2048) {
            throw new IllegalArgumentException("URL length cannot exceed 2048 characters");
        }

        return trimmedUrl;
    }

    /**
     * Проверяет корректность состояния объекта после инициализации.
     */
    private void validateState() {
        if (expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Expiration date cannot be before creation date");
        }

        if (maxClicks <= 0) {
            throw new IllegalArgumentException("Max clicks must be positive");
        }

        if (maxClicks > 1_000_000) {
            throw new IllegalArgumentException("Max clicks cannot exceed 1,000,000");
        }
    }

    /**
     * Увеличивает счетчик переходов по ссылке.
     * Проверяет доступность ссылки перед увеличением счетчика.
     *
     * @throws IllegalStateException если ссылка неактивна, просрочена или достигнут лимит
     */
    public void incrementClicks() {
        if (!isActive) {
            throw new IllegalStateException("Link is not active");
        }

        if (isExpired()) {
            this.isActive = false;
            throw new IllegalStateException("Link has expired");
        }

        // Проверяем, не достигли ли лимита УЖЕ
        if (currentClicks >= maxClicks) {
            this.isActive = false;
            throw new IllegalStateException("Click limit reached");
        }

        this.currentClicks++;

        // Проверяем, не достигли ли лимита ПОСЛЕ увеличения
        if (currentClicks >= maxClicks) {
            this.isActive = false;
        }
    }

    /**
     * Проверяет, истек ли срок действия ссылки.
     *
     * @return true если срок действия истек, иначе false
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Проверяет, достигнут ли лимит переходов.
     *
     * @return true если лимит достигнут, иначе false
     */
    public boolean hasReachedLimit() {
        return currentClicks >= maxClicks;
    }

    /**
     * Проверяет, доступна ли ссылка для использования.
     * Ссылка доступна если: активна, не просрочена, не достигнут лимит.
     *
     * @return true если ссылка доступна, иначе false
     */
    public boolean canBeAccessed() {
        return isActive && !isExpired() && !hasReachedLimit();
    }

    /**
     * Обновляет максимальное количество переходов.
     * Разрешено ТЗ: пользователь может изменять лимит переходов.
     *
     * @param newMaxClicks новый лимит переходов
     * @throws IllegalArgumentException если новый лимит некорректен
     */
    public void updateMaxClicks(int newMaxClicks) {
        if (newMaxClicks <= 0) {
            throw new IllegalArgumentException("Max clicks must be positive");
        }

        if (newMaxClicks > 1_000_000) {
            throw new IllegalArgumentException("Max clicks cannot exceed 1,000,000");
        }

        if (newMaxClicks < currentClicks) {
            throw new IllegalArgumentException(
                    String.format("New max clicks (%d) cannot be less than current clicks (%d)",
                            newMaxClicks, currentClicks)
            );
        }

        this.maxClicks = newMaxClicks;

        // Реактивация ссылки, если она была деактивирована из-за лимита
        if (!isActive && !isExpired()) {
            this.isActive = true;
        }
    }

    /**
     * Деактивирует ссылку (делает ее недоступной).
     * Используется при ручном удалении или системных операциях.
     */
    public void deactivate() {
        this.isActive = false;
    }

    // ==================== ГЕТТЕРЫ ====================

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public int getMaxClicks() {
        return maxClicks;
    }

    public int getCurrentClicks() {
        return currentClicks;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getDescription() {
        return description;
    }

    // ==================== УТИЛИТНЫЕ МЕТОДЫ ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return id.equals(link.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "Link{id=%s, shortCode='%s', originalUrl='%s', expiresAt=%s, " +
                        "currentClicks=%d, maxClicks=%d, isActive=%s, userId=%s}",
                id,
                shortCode,
                originalUrl.length() > 30 ? originalUrl.substring(0, 27) + "..." : originalUrl,
                expiresAt,
                currentClicks,
                maxClicks,
                isActive,
                userId
        );
    }

    /**
     * Создает детализированное строковое представление для отладки.
     *
     * @return детализированное описание ссылки
     */
    public String toDetailedString() {
        long hoursLeft = java.time.Duration.between(LocalDateTime.now(), expiresAt).toHours();
        double usagePercentage = maxClicks > 0 ? (currentClicks * 100.0) / maxClicks : 0;

        return String.format(
                "Short Code: %s%n" +
                        "Original URL: %s%n" +
                        "Created: %s%n" +
                        "Expires: %s (in %d hours)%n" +
                        "Clicks: %d / %d (%.1f%%)%n" +
                        "Status: %s%n" +
                        "Description: %s%n" +
                        "User ID: %s",
                shortCode,
                originalUrl,
                createdAt,
                expiresAt,
                Math.max(0, hoursLeft),
                currentClicks,
                maxClicks,
                usagePercentage,
                getStatusDescription(),
                description.isEmpty() ? "No description" : description,
                userId
        );
    }

    /**
     * Возвращает текстовое описание статуса ссылки.
     *
     * @return описание статуса
     */
    public String getStatusDescription() {
        if (isExpired()) {
            return "EXPIRED";
        } else if (hasReachedLimit()) {
            return "LIMIT REACHED";
        } else if (!isActive) {
            return "INACTIVE (manually deactivated)";
        } else if (canBeAccessed()) {
            return "ACTIVE";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Проверяет, принадлежит ли ссылка указанному пользователю.
     *
     * @param userId UUID пользователя для проверки
     * @return true если пользователь является владельцем, иначе false
     */
    public boolean isOwnedBy(UUID userId) {
        return this.userId.equals(userId);
    }

    /**
     * Возвращает процент использования лимита переходов.
     *
     * @return процент использования (0-100)
     */
    public double getUsagePercentage() {
        if (maxClicks == 0) return 0;
        return Math.min(100.0, (currentClicks * 100.0) / maxClicks);
    }

    /**
     * Возвращает оставшееся время жизни в часах.
     *
     * @return количество оставшихся часов (может быть отрицательным, если срок истек)
     */
    public long getHoursRemaining() {
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toHours();
    }

    /**
     * Проверяет, скоро ли истечет срок действия ссылки.
     *
     * @param thresholdHours порог в часах
     * @return true если до истечения осталось меньше thresholdHours
     */
    public boolean isExpiringSoon(int thresholdHours) {
        long hoursRemaining = getHoursRemaining();

        // Если осталось меньше часа, проверяем минуты
        if (hoursRemaining == 0) {
            long minutesRemaining = java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
            return minutesRemaining > 0 && minutesRemaining <= (thresholdHours * 60);
        }

        return hoursRemaining > 0 && hoursRemaining <= thresholdHours;
    }

    /**
     * Проверяет, близко ли достижение лимита переходов.
     *
     * @param thresholdPercent порог в процентах
     * @return true если использовано больше thresholdPercent лимита
     */
    public boolean isNearLimit(int thresholdPercent) {
        return getUsagePercentage() >= thresholdPercent;
    }
}
