package com.shortener.unit;

import com.shortener.core.exception.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void testLinkExpiredException() {
        LinkExpiredException exception = new LinkExpiredException("Test message");

        assertEquals("Test message", exception.getMessage());
        assertNull(exception.getCause());

        Exception cause = new RuntimeException("Cause");
        LinkExpiredException exceptionWithCause = new LinkExpiredException("Message", cause);

        assertEquals("Message", exceptionWithCause.getMessage());
        assertSame(cause, exceptionWithCause.getCause());
    }

    @Test
    void testLinkNotFoundException() {
        LinkNotFoundException exception = new LinkNotFoundException("Test message");

        assertEquals("Test message", exception.getMessage());
        assertNull(exception.getCause());

        Exception cause = new RuntimeException("Cause");
        LinkNotFoundException exceptionWithCause = new LinkNotFoundException("Message", cause);

        assertEquals("Message", exceptionWithCause.getMessage());
        assertSame(cause, exceptionWithCause.getCause());
    }

    @Test
    void testAccessDeniedException() {
        AccessDeniedException exception = new AccessDeniedException("Test message");

        assertEquals("Test message", exception.getMessage());
        assertNull(exception.getCause());

        Exception cause = new RuntimeException("Cause");
        AccessDeniedException exceptionWithCause = new AccessDeniedException("Message", cause);

        assertEquals("Message", exceptionWithCause.getMessage());
        assertSame(cause, exceptionWithCause.getCause());
    }
}
