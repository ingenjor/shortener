package com.shortener.core.service;

import com.shortener.core.domain.Link;
import com.shortener.core.domain.User;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationService {
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void notifyLinkCreated(User user, Link link, int defaultTtlHours) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("✅ LINK CREATED SUCCESSFULLY");
        System.out.println("=".repeat(50));
        System.out.println("User ID: " + user.getId());
        System.out.println("Short Code: " + link.getShortCode());
        System.out.println("Original URL: " + link.getOriginalUrl());
        System.out.println("Expires At: " + link.getExpiresAt().format(formatter));
        System.out.println("Max Clicks: " + link.getMaxClicks());
        System.out.println("Description: " + link.getDescription());
        System.out.println("-".repeat(50));
        System.out.println("⚠️  This link will expire in " + defaultTtlHours + " hours");
        System.out.println("=".repeat(50) + "\n");
    }

    public void notifyLinkExpired(User user, Link link) {
        String message = String.format(
                "\n⚠️  NOTIFICATION: Link '%s' has expired on %s. " +
                        "Current clicks: %d/%d. Please create a new link if needed.\n",
                link.getShortCode(),
                link.getExpiresAt().format(formatter),
                link.getCurrentClicks(),
                link.getMaxClicks()
        );
        System.out.println(message);

        if (user.getNotificationEmail() != null) {
            System.out.println("📧 Notification email sent to: " + user.getNotificationEmail());
        }
    }

    public void notifyLinkLimitReached(User user, Link link) {
        String message = String.format(
                "\n⚠️  NOTIFICATION: Link '%s' has reached its click limit (%d/%d). " +
                        "The link is now inactive. Please create a new link if needed.\n",
                link.getShortCode(),
                link.getCurrentClicks(),
                link.getMaxClicks()
        );
        System.out.println(message);

        if (user.getNotificationEmail() != null) {
            System.out.println("📧 Notification email sent to: " + user.getNotificationEmail());
        }
    }

    public void notifyLinkNearLimit(User user, Link link, int thresholdPercent) {
        double percentage = (link.getCurrentClicks() * 100.0) / link.getMaxClicks();
        if (percentage >= thresholdPercent) {
            String message = String.format(
                    "\nℹ️  INFO: Link '%s' is near its click limit: %d/%d (%.1f%%).\n",
                    link.getShortCode(),
                    link.getCurrentClicks(),
                    link.getMaxClicks(),
                    percentage
            );
            System.out.println(message);
        }
    }

    public void notifyLinksCleanup(List<Link> expiredLinks) {
        if (!expiredLinks.isEmpty()) {
            System.out.println("\n🧹 Cleanup: Removed " + expiredLinks.size() + " expired links:");
            expiredLinks.forEach(link ->
                    System.out.println("  - " + link.getShortCode() + " (expired: " +
                            link.getExpiresAt().format(formatter) + ")")
            );
            System.out.println();
        }
    }

    public void showWelcomeMessage(int defaultTtlHours) {
        System.out.println("=".repeat(50));
        System.out.println("    🔗 SHORTENER SERVICE v1.0.0");
        System.out.println("=".repeat(50));
        System.out.println("A powerful URL shortener with:");
        System.out.println("• Automatic expiration (" + defaultTtlHours + " hours) - FIXED by system");
        System.out.println("• Click limit tracking");
        System.out.println("• User isolation by UUID");
        System.out.println("=".repeat(50));
        System.out.println("📝 Type 'help' for available commands");
        System.out.println("❌ Type 'exit' to quit");
        System.out.println("=".repeat(50) + "\n");
    }

    public void showErrorMessage(String message) {
        System.out.println("\n❌ ERROR: " + message + "\n");
    }

    public void showInfoMessage(String message) {
        System.out.println("\nℹ️  INFO: " + message + "\n");
    }

    public void showSuccessMessage(String message) {
        System.out.println("\n✅ SUCCESS: " + message + "\n");
    }

    public void showHelp(int defaultTtlHours, int defaultMaxClicks) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📚 AVAILABLE COMMANDS");
        System.out.println("=".repeat(60));

        System.out.println("\n👤 USER MANAGEMENT:");
        System.out.println("  login [UUID]           - Login with existing UUID (optional)");
        System.out.println("  whoami                 - Show current user info");
        System.out.println("  set-email <email>      - Set notification email");

        System.out.println("\n🔗 LINK MANAGEMENT:");
        System.out.println("  create <URL> [max] [desc] - Create short link");
        System.out.println("    Example: create https://example.com 50 \"My link\"");
        System.out.println("  list                   - List all your links");
        System.out.println("  stats <code>           - Show detailed statistics");
        System.out.println("  edit <code> <newMax>   - Update click limit");
        System.out.println("  delete <code>          - Delete a link");
        System.out.println("  goto <code>            - Open link in browser");

        System.out.println("\n⚡ QUICK ACTIONS:");
        System.out.println("  <short_code>           - Just type the code to open link");

        System.out.println("\n🛠️  SYSTEM:");
        System.out.println("  help                   - Show this help");
        System.out.println("  cleanup                - Manual cleanup of expired links");
        System.out.println("  exit                   - Exit application");

        System.out.println("\n" + "-".repeat(60));
        System.out.println("⚙️  CONFIGURATION:");
        System.out.println("  • Default TTL: " + defaultTtlHours + " hours (SYSTEM SETTING - CANNOT BE CHANGED)");
        System.out.println("  • Default max clicks: " + defaultMaxClicks);
        System.out.println("  • Auto-cleanup: every 5 minutes");
        System.out.println("=".repeat(60) + "\n");
    }
}
