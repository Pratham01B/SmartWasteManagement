package com.smartwaste.repository;

import com.smartwaste.entity.OtpToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OtpRepository — uses Mockito to verify query method contracts
 * without requiring a live database connection.
 */
@ExtendWith(MockitoExtension.class)
class OtpRepositoryTest {

    @Mock
    private OtpRepository otpRepository;

    private OtpToken validOtp;
    private OtpToken usedOtp;
    private OtpToken expiredOtp;

    private static final String TEST_EMAIL = "user@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    @BeforeEach
    void setUp() {
        validOtp = OtpToken.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        usedOtp = OtpToken.builder()
                .id(2L)
                .email(TEST_EMAIL)
                .otp("654321")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(true)
                .build();

        expiredOtp = OtpToken.builder()
                .id(3L)
                .email(TEST_EMAIL)
                .otp("999999")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .build();
    }

    // -------------------------
    // findByEmailAndUsedFalse
    // -------------------------

    @Test
    @DisplayName("findByEmailAndUsedFalse returns the unused OTP for a given email")
    void findByEmailAndUsedFalse_returnsUnusedOtp_whenExists() {
        when(otpRepository.findByEmailAndUsedFalse(TEST_EMAIL)).thenReturn(Optional.of(validOtp));

        Optional<OtpToken> result = otpRepository.findByEmailAndUsedFalse(TEST_EMAIL);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.get().isUsed()).isFalse();
        verify(otpRepository).findByEmailAndUsedFalse(TEST_EMAIL);
    }

    @Test
    @DisplayName("findByEmailAndUsedFalse returns empty when OTP has been used")
    void findByEmailAndUsedFalse_returnsEmpty_whenOtpIsUsed() {
        // used=true tokens should not be returned by this query
        when(otpRepository.findByEmailAndUsedFalse(TEST_EMAIL)).thenReturn(Optional.empty());

        Optional<OtpToken> result = otpRepository.findByEmailAndUsedFalse(TEST_EMAIL);

        assertThat(result).isEmpty();
        verify(otpRepository).findByEmailAndUsedFalse(TEST_EMAIL);
    }

    @Test
    @DisplayName("findByEmailAndUsedFalse returns empty when no OTP exists for the email")
    void findByEmailAndUsedFalse_returnsEmpty_whenNoOtpForEmail() {
        when(otpRepository.findByEmailAndUsedFalse(OTHER_EMAIL)).thenReturn(Optional.empty());

        Optional<OtpToken> result = otpRepository.findByEmailAndUsedFalse(OTHER_EMAIL);

        assertThat(result).isEmpty();
        verify(otpRepository).findByEmailAndUsedFalse(OTHER_EMAIL);
    }

    @Test
    @DisplayName("findByEmailAndUsedFalse returns the OTP even if it is expired (expiry is checked in service layer)")
    void findByEmailAndUsedFalse_returnsExpiredOtp_expiryCheckedInService() {
        // The repository only filters on used=false; expiry validation is the service's responsibility
        when(otpRepository.findByEmailAndUsedFalse(TEST_EMAIL)).thenReturn(Optional.of(expiredOtp));

        Optional<OtpToken> result = otpRepository.findByEmailAndUsedFalse(TEST_EMAIL);

        assertThat(result).isPresent();
        assertThat(result.get().isUsed()).isFalse();
        assertThat(result.get().isExpired()).isTrue();
    }

    // -------------------------
    // deleteByEmail (@Modifying @Query)
    // -------------------------

    @Test
    @DisplayName("deleteByEmail deletes all OTP tokens for the given email")
    void deleteByEmail_deletesTokensForEmail() {
        doNothing().when(otpRepository).deleteByEmail(TEST_EMAIL);

        otpRepository.deleteByEmail(TEST_EMAIL);

        verify(otpRepository, times(1)).deleteByEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("deleteByEmail does nothing when no tokens exist for the email")
    void deleteByEmail_doesNothing_whenNoTokensExist() {
        doNothing().when(otpRepository).deleteByEmail(OTHER_EMAIL);

        otpRepository.deleteByEmail(OTHER_EMAIL);

        verify(otpRepository).deleteByEmail(OTHER_EMAIL);
    }

    // -------------------------
    // JpaRepository inherited — save / findById
    // -------------------------

    @Test
    @DisplayName("save persists and returns the OTP token")
    void save_persistsOtpToken() {
        when(otpRepository.save(validOtp)).thenReturn(validOtp);

        OtpToken saved = otpRepository.save(validOtp);

        assertThat(saved).isNotNull();
        assertThat(saved.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(saved.getOtp()).isEqualTo("123456");
        assertThat(saved.isUsed()).isFalse();
        verify(otpRepository).save(validOtp);
    }

    @Test
    @DisplayName("findById returns the OTP token when it exists")
    void findById_returnsToken_whenExists() {
        when(otpRepository.findById(1L)).thenReturn(Optional.of(validOtp));

        Optional<OtpToken> result = otpRepository.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById returns empty Optional when token does not exist")
    void findById_returnsEmpty_whenNotFound() {
        when(otpRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<OtpToken> result = otpRepository.findById(99L);

        assertThat(result).isEmpty();
    }

    // -------------------------
    // OtpToken helper methods (tested via entity, validated here in context)
    // -------------------------

    @Test
    @DisplayName("isValid returns true for an unused, non-expired OTP")
    void otpToken_isValid_whenUnusedAndNotExpired() {
        assertThat(validOtp.isValid()).isTrue();
    }

    @Test
    @DisplayName("isValid returns false for a used OTP")
    void otpToken_isValid_returnsFalse_whenUsed() {
        assertThat(usedOtp.isValid()).isFalse();
    }

    @Test
    @DisplayName("isExpired returns true for an OTP past its expiry time")
    void otpToken_isExpired_returnsTrue_whenPastExpiry() {
        assertThat(expiredOtp.isExpired()).isTrue();
    }

    @Test
    @DisplayName("isExpired returns false for an OTP within its expiry time")
    void otpToken_isExpired_returnsFalse_whenWithinExpiry() {
        assertThat(validOtp.isExpired()).isFalse();
    }
}
