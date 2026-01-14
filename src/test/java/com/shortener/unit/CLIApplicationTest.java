package com.shortener.unit;

import com.shortener.cli.CLIApplication;
import com.shortener.infra.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class CLIApplicationTest {

    @Test
    void testCLIApplicationSingleton() throws Exception {
        // Test that getInstance returns the same instance
        CLIApplication app1 = null;
        CLIApplication app2 = null;

        try {
            // Reset singleton for test
            Field instanceField = CLIApplication.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);

            // Create new instance
            AppConfig config = AppConfig.getInstance();
            CLIApplication app = new CLIApplication(config);

            // Get instance via static method
            app1 = CLIApplication.getInstance();
            app2 = CLIApplication.getInstance();

            assertNotNull(app1);
            assertSame(app1, app2);
        } finally {
            // Cleanup
            if (app1 != null) {
                // Close resources if needed
            }
        }
    }

    @Test
    void testCLIApplicationInitialization() {
        // Test that CLIApplication can be initialized without errors
        assertDoesNotThrow(() -> {
            AppConfig config = AppConfig.getInstance();
            CLIApplication app = new CLIApplication(config);
            assertNotNull(app);
        });
    }
}
