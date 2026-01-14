package com.shortener.unit;

import com.shortener.cli.CLIApplication;
import com.shortener.cli.CommandProcessor;
import com.shortener.core.domain.User;
import com.shortener.core.service.LinkService;
import com.shortener.core.service.NotificationService;
import com.shortener.core.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandProcessorTest {
    private LinkService linkService;
    private UserService userService;
    private NotificationService notificationService;
    private CLIApplication cliApp;
    private CommandProcessor commandProcessor;

    @BeforeEach
    void setUp() {
        linkService = mock(LinkService.class);
        userService = mock(UserService.class);
        notificationService = mock(NotificationService.class);
        cliApp = mock(CLIApplication.class);

        commandProcessor = new CommandProcessor(
                linkService, userService, notificationService, cliApp
        );
    }

    @Test
    void testProcessUnknownCommand() {
        User user = new User();
        commandProcessor.process("unknowncommand", user);

        verify(notificationService, times(1))
                .showErrorMessage("Unknown command: 'unknowncommand'. Type 'help' for available commands.");
    }

    @Test
    void testProcessHelpCommand() {
        User user = new User();
        when(cliApp.getDefaultTtlHours()).thenReturn(24);
        when(cliApp.getDefaultMaxClicks()).thenReturn(100);

        commandProcessor.process("help", user);

        verify(notificationService, times(1))
                .showHelp(24, 100);
    }

    @Test
    void testProcessEmptyCommand() {
        User user = new User();
        commandProcessor.process("", user);
        commandProcessor.process("   ", user);
        commandProcessor.process(null, user);

        // Пустая команда не должна вызывать ошибку
        verify(notificationService, never()).showErrorMessage(anyString());
    }

    @Test
    void testProcessWhoamiWhenNotLoggedIn() {
        commandProcessor.process("whoami", null);

        verify(notificationService, times(1))
                .showInfoMessage("Not logged in (guest mode)");
    }

    @Test
    void testIsShortCodeValidShortCode() throws Exception {
        // Use reflection to test private method
        Method method = CommandProcessor.class.getDeclaredMethod("isShortCode", String.class);
        method.setAccessible(true);

        // Test that actual short codes are recognized
        assertTrue((boolean) method.invoke(commandProcessor, "abc123"));
        assertTrue((boolean) method.invoke(commandProcessor, "AbCdEf"));
        assertTrue((boolean) method.invoke(commandProcessor, "123456"));
        assertTrue((boolean) method.invoke(commandProcessor, "a1b2c3"));

        // Commands should NOT be recognized as short codes
        assertFalse((boolean) method.invoke(commandProcessor, "help"));
        assertFalse((boolean) method.invoke(commandProcessor, "HELP"));
        assertFalse((boolean) method.invoke(commandProcessor, "exit"));
        assertFalse((boolean) method.invoke(commandProcessor, "create"));
        assertFalse((boolean) method.invoke(commandProcessor, "list"));
    }

    @Test
    void testIsShortCodeInvalidShortCode() throws Exception {
        // Use reflection to test private method
        Method method = CommandProcessor.class.getDeclaredMethod("isShortCode", String.class);
        method.setAccessible(true);

        // Too short
        assertFalse((boolean) method.invoke(commandProcessor, "abc"));

        // Too long
        assertFalse((boolean) method.invoke(commandProcessor, "abc12345678"));

        // Contains invalid characters
        assertFalse((boolean) method.invoke(commandProcessor, "abc-123"));
        assertFalse((boolean) method.invoke(commandProcessor, "abc_123"));
        assertFalse((boolean) method.invoke(commandProcessor, "abc 123"));
    }

    @Test
    void testProcessWithShortCodeWhenNotLoggedIn() {
        // When user is null and input looks like a short code
        commandProcessor.process("abc123", null);

        // Should show error message about needing to login
        verify(notificationService, times(1))
                .showErrorMessage("Please login first to use short codes directly.");
    }

    @Test
    void testProcessWithValidHelpCommand() {
        User user = new User();
        when(cliApp.getDefaultTtlHours()).thenReturn(24);
        when(cliApp.getDefaultMaxClicks()).thenReturn(100);

        // Test help command
        commandProcessor.process("help", user);

        verify(notificationService, times(1))
                .showHelp(24, 100);
    }

    @Test
    void testProcessLoginCommandWithoutUUID() {
        User mockUser = mock(User.class);
        when(userService.createUser()).thenReturn(mockUser);

        commandProcessor.process("login", null);

        verify(userService, times(1)).createUser();
        verify(cliApp, times(1)).setCurrentUser(mockUser);
    }

    @Test
    void testProcessLoginCommandWithInvalidUUID() {
        commandProcessor.process("login invalid-uuid", null);

        verify(notificationService, times(1))
                .showErrorMessage("Invalid UUID format");
    }

    @Test
    void testProcessLoginCommandWhenAlreadyLoggedIn() {
        User currentUser = new User();
        commandProcessor.process("login", currentUser);

        verify(notificationService, times(1))
                .showInfoMessage("Already logged in as: " + currentUser.getId());
    }
}
