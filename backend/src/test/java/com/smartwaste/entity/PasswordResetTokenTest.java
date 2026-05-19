package com.smartwaste.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the PasswordResetToken entity.
 * Covers builder construction, field access, default values, and expiry logic.
 */
class PasswordResetTokenTest {

    private static final String SAMPLE_TOKEN = "abc123-reset-token";
    private static final String SAMPLE_EMAIL = "user@example.com";
    private static final LocalDateTime FUTURE = LocalDateTime.now().plusHours(1);
    private static final LocalDateTime PAST   = LocalDateTime.now().minusHours(1);

    private PasswordResetToken token;

    @BeforeEach
    void setUp() {
        token = PasswordResetToken.builder()
                .token(SAMPLE_TOKEN)
                .email(SAMPLE_EMAIL)
                .expiresAt(FUTURE)
                .build();
    }

    // -------------------------
    // Builder construction
    // -------------------------

    @Nested
    @DisplayName("Builder construction")
    class BuilderConstruction {

        @Test
        @DisplayName("builds a token with all fields set correctly")
        void buildsWithAllFields() {
            assertThat(token.getToken()).isEqualTo(SAMPLE_TOKEN);
            assertThat(token.getEmail()).isEqualTo(SAMPLE_EMAIL);
            assertThat(token.getExpiresAt()).isEqualTo(FUTURE);
        }

        @Test
        @DisplayName("id is null before persistence")
        void idIsNullBeforePersistence() {
            assertThat(token.getId()).isNull();
        }

        @Test
        @DisplayName("used defaults to false when not explicitly set")
        void usedDefaultsToFalse() {
            assertThat(token.isUsed()).isFalse();
        }

        @Test
        @DisplayName("builder can explicitly set used to true")
        void builderCanSetUsedToTrue() {
            PasswordResetToken usedToken = PasswordResetToken.builder()
                    .token("tok")
                    .email("a@b.com")
                    .expiresAt(FUTURE)
                    .used(true)
                    .build();
            assertThat(usedToken.isUsed()).isTrue();
        }
    }

    // -------------------------
    // Setters (via @Data)
    // -------------------------

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("setToken() updates the token value")
        void setTokenUpdatesValue() {
            token.setToken("new-token-xyz");
            assertThat(token.getToken()).isEqualTo("new-token-xyz");
        }

        @Test
        @DisplayName("setEmail() updates the email value")
        void setEmailUpdatesValue() {
            token.setEmail("new@example.com");
            assertThat(token.getEmail()).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("setExpiresAt() updates the expiry time")
        void setExpiresAtUpdatesValue() {
            LocalDateTime newExpiry = LocalDateTime.now().plusDays(1);
            token.setExpiresAt(newExpiry);
            assertThat(token.getExpiresAt()).isEqualTo(newExpiry);
        }

        @Test
        @DisplayName("setUsed(true) marks the token as consumed")
        void setUsedMarksAsConsumed() {
            token.setUsed(true);
            assertThat(token.isUsed()).isTrue();
        }

        @Test
        @DisplayName("setUsed(false) marks the token as not consumed")
        void setUsedFalseMarksAsNotConsumed() {
            token.setUsed(true);
            token.setUsed(false);
            assertThat(token.isUsed()).isFalse();
        }
    }

    // -------------------------
    // Expiry checks
    // -------------------------

    @Nested
    @DisplayName("Expiry checks")
    class ExpiryChecks {

        @Test
        @DisplayName("token with future expiresAt is not yet expired")
        void futureExpiryIsNotExpired() {
            assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("token with past expiresAt is expired")
        void pastExpiryIsExpired() {
            token.setExpiresAt(PAST);
            assertThat(token.getExpiresAt()).isBefore(LocalDateTime.now());
        }

        @Test
        @DisplayName("a used token with future expiry is still marked used")
        void usedTokenWithFutureExpiry() {
            token.setUsed(true);
            assertThat(token.isUsed()).isTrue();
            assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
        }
    }

    // -------------------------
    // Equality and hashCode (via @Data)
    // -------------------------

    @Nested
    @DisplayName("Equality and hashCode")
    class EqualityAndHashCode {

        @Test
        @DisplayName("two tokens with identical fields are equal")
        void identicalTokensAreEqual() {
            PasswordResetToken other = PasswordResetToken.builder()
                    .token(SAMPLE_TOKEN)
                    .email(SAMPLE_EMAIL)
                    .expiresAt(FUTURE)
                    .build();
            assertThat(token).isEqualTo(other);
        }

        @Test
        @DisplayName("two tokens with different token strings are not equal")
        void differentTokenStringsAreNotEqual() {
            PasswordResetToken other = PasswordResetToken.builder()
                    .token("different-token")
                    .email(SAMPLE_EMAIL)
                    .expiresAt(FUTURE)
                    .build();
            assertThat(token).isNotEqualTo(other);
        }

        @Test
        @DisplayName("equal tokens have the same hashCode")
        void equalTokensHaveSameHashCode() {
            PasswordResetToken other = PasswordResetToken.builder()
                    .token(SAMPLE_TOKEN)
                    .email(SAMPLE_EMAIL)
                    .expiresAt(FUTURE)
                    .build();
            assertThat(token.hashCode()).isEqualTo(other.hashCode());
        }
    }

    // -------------------------
    // No-args constructor (via @NoArgsConstructor)
    // -------------------------

    @Nested
    @DisplayName("No-args constructor")
    class NoArgsConstructorTest {

        @Test
        @DisplayName("no-args constructor creates an instance with null fields and used=false")
        void noArgsConstructorCreatesEmptyInstance() {
            PasswordResetToken empty = new PasswordResetToken();
            assertThat(empty.getId()).isNull();
            assertThat(empty.getToken()).isNull();
            assertThat(empty.getEmail()).isNull();
            assertThat(empty.getExpiresAt()).isNull();
            assertThat(empty.isUsed()).isFalse();
        }
    }

    // -------------------------
    // All-args constructor (via @AllArgsConstructor)
    // -------------------------

    @Nested
    @DisplayName("All-args constructor")
    class AllArgsConstructorTest {

        @Test
        @DisplayName("all-args constructor sets every field correctly")
        void allArgsConstructorSetsAllFields() {
            PasswordResetToken full = new PasswordResetToken(42L, "tok", "e@mail.com", FUTURE, true);
            assertThat(full.getId()).isEqualTo(42L);
            assertThat(full.getToken()).isEqualTo("tok");
            assertThat(full.getEmail()).isEqualTo("e@mail.com");
            assertThat(full.getExpiresAt()).isEqualTo(FUTURE);
            assertThat(full.isUsed()).isTrue();
        }
    }
}
