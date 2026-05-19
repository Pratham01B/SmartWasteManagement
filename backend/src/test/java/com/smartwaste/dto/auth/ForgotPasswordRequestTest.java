package com.smartwaste.dto.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ForgotPasswordRequest DTO validation constraints.
 */
class ForgotPasswordRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // -------------------------
    // Valid cases
    // -------------------------

    @Test
    void validEmail_shouldPassValidation() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@example.com");

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    // -------------------------
    // @NotBlank on email
    // -------------------------

    @Test
    void nullEmail_shouldFailNotBlank() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(null);

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("email")
                        && v.getMessage().equals("Email is required"));
    }

    @Test
    void blankEmail_shouldFailNotBlank() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("   ");

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void emptyEmail_shouldFailNotBlank() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("");

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    // -------------------------
    // @Email format validation
    // -------------------------

    @Test
    void invalidEmailFormat_missingAtSign_shouldFailEmailConstraint() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("notanemail");

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("email")
                        && v.getMessage().equals("Invalid email format"));
    }

    @Test
    void invalidEmailFormat_missingDomain_shouldFailEmailConstraint() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@");

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void invalidEmailFormat_missingLocalPart_shouldFailEmailConstraint() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("@example.com");

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void validEmailWithSubdomain_shouldPassValidation() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@mail.example.co.in");

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    // -------------------------
    // Getter / setter
    // -------------------------

    @Test
    void setAndGetEmail_shouldReturnSameValue() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@domain.com");

        assertThat(request.getEmail()).isEqualTo("test@domain.com");
    }
}
