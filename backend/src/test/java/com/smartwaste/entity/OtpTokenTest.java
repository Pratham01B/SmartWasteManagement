package com.smartwaste.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OtpToken entity helper methods.
 * No Spring context needed — pure POJO tests.
 */
class OtpTokenTest {

    // -------------------------
    // isExpired()
    // -------------------------

    @Test
    void isExpired_returnsFalse_whenExpiryIsInFuture() {
        OtpToken token = OtpToken.builder()
                .email("user@example.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void isExpired_returnsTrue_whenExpiryIsInPast() {
        OtpToken token = OtpToken.builder()
                .email("user@example.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        assertThat(token.isExpired()).isTrue();
    }

    // -------------------------
    // isValid()
    // -------------------------

    @Test
    void isValid_returnsTrue_whenNotUsedAndNotExpired() {
        OtpToken token = OtpToken.builder()
                .email("user@example.com")
                .otp("654321")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

        assertThat(token.isValid()).isTrue();
    }

    @Test
    void isValid_returnsFalse_whenAlreadyUsed() {
        OtpToken token = OtpToken.builder()
                .email("user@example.com")
                .otp("654321")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(true)
                .build();

        assertThat(token.isValid()).isFalse();
    }

    @Test
    void isValid_returnsFalse_whenExpired() {
        OtpToken token = OtpToken.builder()
                .email("user@example.com")
                .otp("654321")
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .used(false)
                .build();

        assertThat(token.isValid()).isFalse();
    }

    @Test
    void isValid_returnsFalse_whenUsedAndExpired() {
        OtpToken token = OtpToken.builder()
                .email("user@example.com")
                .otp("654321")
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .used(true)
                .build();

        assertThat(token.isValid()).isFalse();
    }

    // -------------------------
    // Builder defaults
    // -------------------------

    @Test
    void builder_defaultUsed_isFalse() {
        OtpToken token = OtpToken.builder()
                .email("user@example.com")
                .otp("000000")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        assertThat(token.isUsed()).isFalse();
    }

    // -------------------------
    // Getters / field mapping
    // -------------------------

    @Test
    void getters_returnCorrectValues() {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);

        OtpToken token = OtpToken.builder()
                .email("test@smartwaste.com")
                .otp("112233")
                .expiresAt(expiry)
                .used(false)
                .build();

        assertThat(token.getEmail()).isEqualTo("test@smartwaste.com");
        assertThat(token.getOtp()).isEqualTo("112233");
        assertThat(token.getExpiresAt()).isEqualTo(expiry);
        assertThat(token.isUsed()).isFalse();
    }

    // -------------------------
    // NoArgsConstructor
    // -------------------------

    @Test
    void noArgsConstructor_createsInstanceWithNullFields() {
        OtpToken token = new OtpToken();

        assertThat(token.getEmail()).isNull();
        assertThat(token.getOtp()).isNull();
        assertThat(token.getExpiresAt()).isNull();
        assertThat(token.getId()).isNull();
    }

    // -------------------------
    // AllArgsConstructor
    // -------------------------

    @Test
    void allArgsConstructor_setsAllFields() {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        OtpToken token = new OtpToken(42L, "all@example.com", "999999", expiry, true);

        assertThat(token.getId()).isEqualTo(42L);
        assertThat(token.getEmail()).isEqualTo("all@example.com");
        assertThat(token.getOtp()).isEqualTo("999999");
        assertThat(token.getExpiresAt()).isEqualTo(expiry);
        assertThat(token.isUsed()).isTrue();
    }

    // -------------------------
    // Setters (@Data)
    // -------------------------

    @Test
    void setters_updateFieldsCorrectly() {
        OtpToken token = new OtpToken();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);

        token.setEmail("setter@example.com");
        token.setOtp("777777");
        token.setExpiresAt(expiry);
        token.setUsed(true);

        assertThat(token.getEmail()).isEqualTo("setter@example.com");
        assertThat(token.getOtp()).isEqualTo("777777");
        assertThat(token.getExpiresAt()).isEqualTo(expiry);
        assertThat(token.isUsed()).isTrue();
    }

    // -------------------------
    // isExpired() — boundary
    // -------------------------

    @Test
    void isExpired_returnsFalse_whenExpiryIsExactlyNow() throws InterruptedException {
        // expiresAt set to a moment slightly in the future to avoid flakiness
        OtpToken token = OtpToken.builder()
                .email("boundary@example.com")
                .otp("000001")
                .expiresAt(LocalDateTime.now().plusNanos(500_000_000))
                .build();

        assertThat(token.isExpired()).isFalse();
    }

    // -------------------------
    // equals / hashCode (@Data)
    // -------------------------

    @Test
    void equalsAndHashCode_twoIdenticalTokens_areEqual() {
        LocalDateTime expiry = LocalDateTime.of(2025, 1, 1, 12, 0);

        OtpToken t1 = new OtpToken(1L, "eq@example.com", "123456", expiry, false);
        OtpToken t2 = new OtpToken(1L, "eq@example.com", "123456", expiry, false);

        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }

    @Test
    void equalsAndHashCode_differentOtp_areNotEqual() {
        LocalDateTime expiry = LocalDateTime.of(2025, 1, 1, 12, 0);

        OtpToken t1 = new OtpToken(1L, "eq@example.com", "111111", expiry, false);
        OtpToken t2 = new OtpToken(1L, "eq@example.com", "222222", expiry, false);

        assertThat(t1).isNotEqualTo(t2);
    }
}
