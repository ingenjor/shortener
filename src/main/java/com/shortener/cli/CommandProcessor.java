package com.shortener.cli;

import com.shortener.core.domain.Link;
import com.shortener.core.domain.User;
import com.shortener.core.service.LinkService;
import com.shortener.core.service.NotificationService;
import com.shortener.core.service.UserService;

import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandProcessor {
    private final LinkService linkService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final CLIApplication cliApp;

    public CommandProcessor(LinkService linkService, UserService userService,
                            NotificationService notificationService, CLIApplication cliApp) {
        this.linkService = linkService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.cliApp = cliApp;
    }

    public void process(String input, User currentUser) {
        // Обработка пустого ввода
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        input = input.trim();

        if (isShortCode(input)) {
            handleShortCode(input, currentUser);
            return;
        }

        String[] parts = input.split("\\s+");
        if (parts.length == 0) return;

        String command = parts[0].toLowerCase();

        // Сначала обрабатываем команду help, так как она может конфликтовать с short code
        if ("help".equals(command)) {
            notificationService.showHelp(cliApp.getDefaultTtlHours(), cliApp.getDefaultMaxClicks());
            return;
        }

        switch (command) {
            case "login":
                handleLogin(parts, currentUser);
                break;
            case "whoami":
                handleWhoami(currentUser);
                break;
            case "create":
                handleCreate(parts, currentUser);
                break;
            case "list":
                handleList(currentUser);
                break;
            case "stats":
                handleStats(parts, currentUser);
                break;
            case "edit":
                handleEdit(parts, currentUser);
                break;
            case "delete":
                handleDelete(parts, currentUser);
                break;
            case "goto":
                handleGoto(parts, currentUser);
                break;
            case "set-email":
                handleSetEmail(parts, currentUser);
                break;
            case "cleanup":
                handleCleanup();
                break;
            default:
                notificationService.showErrorMessage(
                        "Unknown command: '" + command + "'. Type 'help' for available commands."
                );
        }
    }

    private boolean isShortCode(String input) {
        // Проверяем, что это строка из букв и цифр определенной длины
        if (!input.matches("^[a-zA-Z0-9]{5,10}$")) {
            return false;
        }

        // Проверяем, что это не команда
        String lowerInput = input.toLowerCase();
        return !lowerInput.equals("help") &&
                !lowerInput.equals("exit") &&
                !lowerInput.equals("login") &&
                !lowerInput.equals("whoami") &&
                !lowerInput.equals("create") &&
                !lowerInput.equals("list") &&
                !lowerInput.equals("stats") &&
                !lowerInput.equals("edit") &&
                !lowerInput.equals("delete") &&
                !lowerInput.equals("goto") &&
                !lowerInput.equals("set-email") &&
                !lowerInput.equals("cleanup");
    }

    private void handleShortCode(String shortCode, User currentUser) {
        if (currentUser == null) {
            notificationService.showErrorMessage(
                    "Please login first to use short codes directly."
            );
            return;
        }

        try {
            String originalUrl = linkService.getOriginalUrl(shortCode);
            openInBrowser(originalUrl);
        } catch (Exception e) {
            notificationService.showErrorMessage(
                    "Cannot open link '" + shortCode + "': " + e.getMessage()
            );
        }
    }

    private void handleLogin(String[] parts, User currentUser) {
        if (currentUser != null) {
            notificationService.showInfoMessage(
                    "Already logged in as: " + currentUser.getId()
            );
            return;
        }

        User user;
        if (parts.length >= 2) {
            try {
                UUID userId = UUID.fromString(parts[1]);
                user = userService.getOrCreateUser(userId);
            } catch (IllegalArgumentException e) {
                notificationService.showErrorMessage("Invalid UUID format");
                return;
            }
        } else {
            user = userService.createUser();
        }

        cliApp.setCurrentUser(user);
    }

    private void handleWhoami(User currentUser) {
        if (currentUser == null) {
            notificationService.showInfoMessage("Not logged in (guest mode)");
        } else {
            System.out.println("\n" + "=".repeat(40));
            System.out.println("👤 USER INFORMATION");
            System.out.println("=".repeat(40));
            System.out.println("User ID: " + currentUser.getId());
            System.out.println("Created: " + currentUser.getCreatedAt());
            System.out.println("Last Activity: " + currentUser.getLastActivity());

            List<Link> links = linkService.getUserLinks(currentUser.getId());
            System.out.println("Total Links: " + links.size());

            long activeLinks = links.stream().filter(Link::canBeAccessed).count();
            System.out.println("Active Links: " + activeLinks);

            System.out.println("Email: " +
                    (currentUser.getNotificationEmail() != null ?
                            currentUser.getNotificationEmail() : "Not set"));
            System.out.println("=".repeat(40) + "\n");
        }
    }

    private void handleCreate(String[] parts, User currentUser) {
        if (currentUser == null) {
            User newUser = userService.createUser();
            cliApp.setCurrentUser(newUser);
            currentUser = newUser;
            notificationService.showInfoMessage(
                    "New user created with ID: " + newUser.getId()
            );
        }

        if (parts.length < 2) {
            notificationService.showErrorMessage(
                    "Usage: create <URL> [maxClicks] [description]"
            );
            return;
        }

        String url = parts[1];
        Integer maxClicks = parts.length > 2 ? parseInt(parts[2]) : null;

        StringBuilder description = new StringBuilder();
        for (int i = 3; i < parts.length; i++) {
            description.append(parts[i]).append(" ");
        }

        try {
            // TTL ТОЛЬКО из конфигурации (строго по ТЗ)
            Link link = linkService.createLink(
                    currentUser,
                    url,
                    maxClicks,
                    description.toString().trim()
            );

            currentUser.addLink(link.getId());

            notificationService.notifyLinkCreated(currentUser, link, cliApp.getDefaultTtlHours());

        } catch (IllegalArgumentException e) {
            notificationService.showErrorMessage(e.getMessage());
        }
    }

    private void handleList(User currentUser) {
        if (currentUser == null) {
            notificationService.showErrorMessage(
                    "Please login first using 'login' command"
            );
            return;
        }

        List<Link> links = linkService.getUserLinks(currentUser.getId());

        if (links.isEmpty()) {
            notificationService.showInfoMessage("No links found");
            return;
        }

        System.out.println("\n" + "=".repeat(120));
        System.out.printf("%-10s %-50s %-12s %-8s %-8s %-10s %-12s%n",
                "Code", "Original URL", "Expires", "Clicks", "Max", "Status", "Description");
        System.out.println("-".repeat(120));

        for (Link link : links) {
            String status;
            if (!link.isActive()) {
                status = "❌ INACTIVE";
            } else if (link.isExpired()) {
                status = "⏰ EXPIRED";
            } else if (link.hasReachedLimit()) {
                status = "🚫 LIMIT";
            } else {
                status = "✅ ACTIVE";
            }

            String shortUrl = link.getOriginalUrl();
            if (shortUrl.length() > 45) {
                shortUrl = shortUrl.substring(0, 42) + "...";
            }

            String shortDesc = link.getDescription();
            if (shortDesc.length() > 10) {
                shortDesc = shortDesc.substring(0, 7) + "...";
            }

            System.out.printf("%-10s %-50s %-12s %-8d %-8d %-10s %-12s%n",
                    link.getShortCode(),
                    shortUrl,
                    link.getExpiresAt().toLocalDate().toString(),
                    link.getCurrentClicks(),
                    link.getMaxClicks(),
                    status,
                    shortDesc);
        }
        System.out.println("=".repeat(120) + "\n");
    }

    private void handleStats(String[] parts, User currentUser) {
        if (currentUser == null) {
            notificationService.showErrorMessage(
                    "Please login first using 'login' command"
            );
            return;
        }

        if (parts.length < 2) {
            notificationService.showErrorMessage("Usage: stats <shortCode>");
            return;
        }

        String shortCode = parts[1];

        try {
            Map<String, Object> stats = linkService.getStatistics(
                    shortCode, currentUser.getId()
            );

            System.out.println("\n" + "=".repeat(60));
            System.out.println("📊 LINK STATISTICS: " + stats.get("shortCode"));
            System.out.println("=".repeat(60));
            System.out.println("Original URL: " + stats.get("originalUrl"));
            System.out.println("Created: " + stats.get("createdAt"));
            System.out.println("Expires: " + stats.get("expiresAt"));
            System.out.println("Hours Left: " + stats.get("hoursLeft"));
            System.out.println("Clicks: " + stats.get("currentClicks") +
                    "/" + stats.get("maxClicks"));
            System.out.printf("Usage: %.1f%%\n", stats.get("usagePercentage"));
            System.out.println("Status: " +
                    (Boolean.TRUE.equals(stats.get("canBeAccessed")) ?
                            "✅ ACTIVE" : "❌ INACTIVE"));
            System.out.println("Description: " + stats.get("description"));
            System.out.println("=".repeat(60) + "\n");

        } catch (Exception e) {
            notificationService.showErrorMessage(e.getMessage());
        }
    }

    private void handleEdit(String[] parts, User currentUser) {
        if (currentUser == null) {
            notificationService.showErrorMessage(
                    "Please login first using 'login' command"
            );
            return;
        }

        if (parts.length < 3) {
            notificationService.showErrorMessage(
                    "Usage: edit <shortCode> <newMaxClicks>"
            );
            return;
        }

        String shortCode = parts[1];
        Integer newMaxClicks = parseInt(parts[2]);

        if (newMaxClicks == null || newMaxClicks <= 0) {
            notificationService.showErrorMessage(
                    "Max clicks must be a positive number"
            );
            return;
        }

        try {
            Link link = linkService.updateMaxClicks(
                    shortCode, currentUser.getId(), newMaxClicks
            );

            notificationService.showSuccessMessage(
                    "Updated link '" + shortCode + "' to max " + newMaxClicks + " clicks"
            );

            if (link.isActive() && link.hasReachedLimit()) {
                notificationService.showInfoMessage(
                        "Link has been reactivated with new limit"
                );
            }

        } catch (Exception e) {
            notificationService.showErrorMessage(e.getMessage());
        }
    }

    private void handleDelete(String[] parts, User currentUser) {
        if (currentUser == null) {
            notificationService.showErrorMessage(
                    "Please login first using 'login' command"
            );
            return;
        }

        if (parts.length < 2) {
            notificationService.showErrorMessage("Usage: delete <shortCode>");
            return;
        }

        String shortCode = parts[1];

        try {
            linkService.deleteLink(shortCode, currentUser.getId());
            notificationService.showSuccessMessage(
                    "Deleted link: " + shortCode
            );
        } catch (Exception e) {
            notificationService.showErrorMessage(e.getMessage());
        }
    }

    private void handleGoto(String[] parts, User currentUser) {
        if (parts.length < 2) {
            notificationService.showErrorMessage("Usage: goto <shortCode>");
            return;
        }

        String shortCode = parts[1];

        try {
            String originalUrl = linkService.getOriginalUrl(shortCode);
            openInBrowser(originalUrl);
        } catch (Exception e) {
            notificationService.showErrorMessage(
                    "Cannot open link: " + e.getMessage()
            );
        }
    }

    private void handleSetEmail(String[] parts, User currentUser) {
        if (currentUser == null) {
            notificationService.showErrorMessage(
                    "Please login first using 'login' command"
            );
            return;
        }

        if (parts.length < 2) {
            notificationService.showErrorMessage("Usage: set-email <email>");
            return;
        }

        String email = parts[1];

        try {
            userService.setUserEmail(currentUser.getId(), email);
            notificationService.showSuccessMessage(
                    "Notification email set to: " + email
            );
        } catch (Exception e) {
            notificationService.showErrorMessage(e.getMessage());
        }
    }

    private void handleCleanup() {
        List<Link> expiredLinks = linkService.findExpiredLinks();

        if (!expiredLinks.isEmpty()) {
            notificationService.notifyLinksCleanup(expiredLinks);
            notificationService.showSuccessMessage(
                    "Cleanup completed. Found " + expiredLinks.size() + " expired links."
            );
        } else {
            notificationService.showInfoMessage("No expired links found.");
        }
    }

    private void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() &&
                    Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {

                Desktop.getDesktop().browse(new URI(url));
                notificationService.showSuccessMessage(
                        "🌐 Opening: " + url
                );
            } else {
                notificationService.showInfoMessage(
                        "🔗 URL: " + url + "\n" +
                                "(Cannot open browser automatically. Copy the URL above.)"
                );
            }
        } catch (Exception e) {
            notificationService.showErrorMessage(
                    "Failed to open browser: " + e.getMessage()
            );
        }
    }

    private Integer parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
