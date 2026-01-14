package com.shortener.infra.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class AppConfig {
    private static final String DEFAULT_CONFIG_PATH = "config/application.yaml";
    private static AppConfig instance;

    private final Map<String, Object> config;

    private AppConfig() {
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = Files.newInputStream(Paths.get(DEFAULT_CONFIG_PATH));
            config = yaml.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private <T> T getValue(String path, T defaultValue) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = config;

        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.get(parts[i]);
            if (current == null) {
                return defaultValue;
            }
        }

        T value = (T) current.get(parts[parts.length - 1]);
        return value != null ? value : defaultValue;
    }

    // Геттеры для конфигурации

    public int getShortCodeLength() {
        return getValue("link.short-code-length", 7);
    }

    public int getDefaultTtlHours() {
        return getValue("link.default-ttl-hours", 24);
    }

    public int getDefaultMaxClicks() {
        return getValue("link.default-max-clicks", 100);
    }

    public String getGenerationAlgorithm() {
        return getValue("link.generation-algorithm", "BASE62");
    }

    public boolean isExpireNotificationEnabled() {
        return getValue("notification.expire-notification", true);
    }

    public boolean isLimitNotificationEnabled() {
        return getValue("notification.limit-notification", true);
    }

    public int getCleanupIntervalMinutes() {
        return getValue("cleanup.check-interval-minutes", 5);
    }

    public boolean isAutoDeleteExpired() {
        return getValue("cleanup.auto-delete-expired", true);
    }

    public boolean isOwnerOnlyOperations() {
        return getValue("security.owner-only-operations", true);
    }

    public int getUserSessionTtlHours() {
        return getValue("security.user-session-ttl-hours", 168);
    }

    public String getLoggingLevel() {
        return getValue("logging.level", "INFO");
    }

    public String getLoggingFile() {
        return getValue("logging.file", "logs/shortener.log");
    }
}
