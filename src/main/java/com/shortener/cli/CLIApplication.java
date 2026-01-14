package com.shortener.cli;

import com.shortener.core.domain.User;
import com.shortener.core.service.*;
import com.shortener.infra.config.AppConfig;
import com.shortener.infra.scheduler.LinkCleanupScheduler;
import com.shortener.infra.storage.InMemoryLinkRepository;
import com.shortener.infra.storage.InMemoryUserRepository;

import java.util.Scanner;
import java.util.UUID;

public class CLIApplication {
    private final LinkService linkService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final LinkCleanupScheduler cleanupScheduler;
    private final CommandProcessor commandProcessor;
    private final int defaultTtlHours;
    private final int defaultMaxClicks;
    private User currentUser;
    private static CLIApplication instance;

    public CLIApplication(AppConfig config) {
        instance = this;
        this.defaultTtlHours = config.getDefaultTtlHours();
        this.defaultMaxClicks = config.getDefaultMaxClicks();

        // Инициализация репозиториев
        InMemoryLinkRepository linkRepository = new InMemoryLinkRepository();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();

        // Инициализация сервисов
        ShortCodeGenerator codeGenerator = new ShortCodeGenerator(
                ShortCodeGenerator.Algorithm.valueOf(config.getGenerationAlgorithm()),
                config.getShortCodeLength()
        );

        this.linkService = new LinkService(
                linkRepository,
                codeGenerator,
                config.getDefaultTtlHours(),
                config.getDefaultMaxClicks()
        );

        // ИЗМЕНЕНО: передаем int напрямую (теперь UserService ожидает int)
        this.userService = new UserService(
                userRepository,
                config.getUserSessionTtlHours()
        );

        this.notificationService = new NotificationService();
        this.commandProcessor = new CommandProcessor(
                linkService,
                userService,
                notificationService,
                this
        );

        this.cleanupScheduler = new LinkCleanupScheduler(
                linkRepository,
                notificationService,
                config.isAutoDeleteExpired(),
                config.getCleanupIntervalMinutes()
        );
    }

    public static CLIApplication getInstance() {
        return instance;
    }

    public void start() {
        cleanupScheduler.start();
        notificationService.showWelcomeMessage(defaultTtlHours);

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;

            while (running) {
                showPrompt();
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit")) {
                    running = false;
                    continue;
                }

                if (input.equalsIgnoreCase("help")) {
                    notificationService.showHelp(defaultTtlHours, defaultMaxClicks);
                    continue;
                }

                try {
                    commandProcessor.process(input, currentUser);
                    if (currentUser != null) {
                        userService.updateUserActivity(currentUser.getId());
                    }
                } catch (Exception e) {
                    notificationService.showErrorMessage(e.getMessage());
                }
            }
        }

        cleanupScheduler.shutdown();
        System.out.println("\n👋 Thank you for using Shortener Service. Goodbye!\n");
    }

    private void showPrompt() {
        if (currentUser == null) {
            System.out.print("👤 guest> ");
        } else {
            System.out.print("👤 user:" +
                    currentUser.getId().toString().substring(0, 8) + "...> ");
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            notificationService.showSuccessMessage(
                    "Logged in as user: " + user.getId()
            );
        }
    }

    public int getDefaultTtlHours() {
        return defaultTtlHours;
    }

    public int getDefaultMaxClicks() {
        return defaultMaxClicks;
    }

    public static void main(String[] args) {
        try {
            AppConfig config = AppConfig.getInstance();
            CLIApplication app = new CLIApplication(config);
            app.start();
        } catch (Exception e) {
            System.err.println("❌ Failed to start application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
