package com.smartwaste.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    // -----------------------------------------------------------------------
    // handleBadCredentials
    // -----------------------------------------------------------------------

    @Test
    void handleBadCredentials_returns401WithMessage() {
        BadCredentialsException ex = new BadCredentialsException("wrong password");

        ResponseEntity<Map<String, Object>> response = handler.handleBadCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("status", 401);
        assertThat(response.getBody()).containsEntry("message", "Invalid email or password");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    // -----------------------------------------------------------------------
    // handleAccessDenied
    // -----------------------------------------------------------------------

    @Test
    void handleAccessDenied_returns403WithMessage() {
        AccessDeniedException ex = new AccessDeniedException("forbidden");

        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("status", 403);
        assertThat(response.getBody()).containsEntry("message", "Access denied");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    // -----------------------------------------------------------------------
    // handleEmailExists
    // -----------------------------------------------------------------------

    @Test
    void handleEmailExists_returns409WithExceptionMessage() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("test@example.com already registered");

        ResponseEntity<Map<String, Object>> response = handler.handleEmailExists(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("status", 409);
        assertThat(response.getBody()).containsEntry("message", "test@example.com already registered");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    // -----------------------------------------------------------------------
    // handleValidation
    // -----------------------------------------------------------------------

    @Mock
    private BindingResult bindingResult;

    @Mock
    private MethodArgumentNotValidException validationEx;

    @Test
    void handleValidation_returns400WithFieldErrors() {
        FieldError fieldError = new FieldError("registerRequest", "email", "must not be blank");
        when(validationEx.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(validationEx);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsKey("timestamp");

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("errors");
        assertThat(fieldErrors).containsEntry("email", "must not be blank");
    }

    @Test
    void handleValidation_returnsEmptyErrorsWhenNoFieldErrors() {
        when(validationEx.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(validationEx);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("errors");
        assertThat(fieldErrors).isEmpty();
    }

    @Test
    void handleValidation_returnsMultipleFieldErrors() {
        FieldError emailError = new FieldError("registerRequest", "email", "must not be blank");
        FieldError passwordError = new FieldError("registerRequest", "password", "size must be between 8 and 50");
        when(validationEx.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(emailError, passwordError));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(validationEx);

        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("errors");
        assertThat(fieldErrors)
                .containsEntry("email", "must not be blank")
                .containsEntry("password", "size must be between 8 and 50");
    }

    // -----------------------------------------------------------------------
    // handleNotFound
    // -----------------------------------------------------------------------

    @Test
    void handleNotFound_returns404WithExceptionMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Complaint with id 99 not found");

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("message", "Complaint with id 99 not found");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    // -----------------------------------------------------------------------
    // handleGeneric
    // -----------------------------------------------------------------------

    @Test
    void handleGeneric_returns500WithExceptionMessage() {
        Exception ex = new RuntimeException("unexpected failure");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsKey("timestamp");

        String message = (String) response.getBody().get("message");
        assertThat(message).contains("unexpected failure");
    }

    @Test
    void handleGeneric_messageContainsInternalErrorPrefix() {
        Exception ex = new IllegalStateException("state problem");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        String message = (String) response.getBody().get("message");
        assertThat(message).startsWith("Internal error:");
    }
}
