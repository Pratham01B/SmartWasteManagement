package com.smartwaste.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResourceNotFoundException.
 */
class ResourceNotFoundExceptionTest {

    @Test
    void constructor_shouldSetMessage() {
        String message = "Complaint not found: 42";

        ResourceNotFoundException ex = new ResourceNotFoundException(message);

        assertEquals(message, ex.getMessage());
    }

    @Test
    void shouldBeInstanceOfRuntimeException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("test");

        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void constructor_withNullMessage_shouldNotThrow() {
        assertDoesNotThrow(() -> new ResourceNotFoundException(null));
    }

    @Test
    void constructor_withEmptyMessage_shouldPreserveEmptyString() {
        ResourceNotFoundException ex = new ResourceNotFoundException("");

        assertEquals("", ex.getMessage());
    }
}
