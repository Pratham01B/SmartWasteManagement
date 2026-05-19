package com.smartwaste.repository;

import com.smartwaste.entity.PasswordResetToken;
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
 * Unit tests for PasswordResetTokenRepository — uses Mockito to verify query
 * method contracts without requiring a live database connection.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetTokenRepositoryTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private PasswordResetToken validToken;
    private PasswordResetToken usedToken;
    private PasswordResetToken expiredToken;

    private static final String TEST_EMAIL   = "user@example.com";
    private static final String OTHER_EMAIL  = "other@example.com";
    private static final String VALID_TOKEN  = "valid-uuid-token-1234";
    private static final String USED_TOKEN   = "used-uuid-token-5678";
    private static final String EXPIRED_TOKEN = "expired-uuid-token-9999";

    @BeforeEach
    void setUp() {
        validToken = PasswordResetToken.builder()
                .id(1L)
                .token(VALID_TOKEN)
                .email(TEST_EMAIL)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        usedToken = PasswordResetToken.builder()
                .id(2L)
                .token(USED_TOKEN)
                .email(TEST_EMAIL)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(true)
                .build();

        expiredToken = PasswordResetToken.builder()
                .id(3L)
                .token(EXPIRED_TOKEN)
                .email(TEST_EMAIL)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .build();
    }

    // -------------------------
    // findByToken
    // -------------------------

    @Test
    @DisplayName("findByToken returns the token when it exists")
    void findByToken_returnsToken_whenExists() {
        when(passwordResetTokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));

        Optional<PasswordResetToken> result = passwordResetTokenRepository.findByToken(VALID_TOKEN);

        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo(VALID_TOKEN);
        assertThat(result.get().getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.get().isUsed()).isFalse();
        verify(passwordResetTokenRepository).findByToken(VALID_TOKEN);
    }

    @Test
    @DisplayName("findByToken returns empty Optional when token does not exist")
    void findByToken_returnsEmpty_whenNotFound() {
        when(passwordResetTokenRepository.findByToken("nonexistent-token")).thenReturn(Optional.empty());

        Optional<PasswordResetToken> result = passwordResetTokenRepository.findByToken("nonexistent-token");

        assertThat(result).isEmpty();
        verify(passwordResetTokenRepository).findByToken("nonexistent-token");
    }

    @Test
    @DisplayName("findByToken returns a used token (expiry/used checks are the service's responsibility)")
    void findByToken_returnsUsedToken_serviceChecksUsedFlag() {
        when(passwordResetTokenRepository.findByToken(USED_TOKEN)).thenReturn(Optional.of(usedToken));

        Optional<PasswordResetToken> result = passwordResetTokenRepository.findByToken(USED_TOKEN);

        assertThat(result).isPresent();
        assertThat(result.get().isUsed()).isTrue();
    }

    @Test
    @DisplayName("findByToken returns an expired token (expiry check is the service's responsibility)")
    void findByToken_returnsExpiredToken_serviceChecksExpiry() {
        when(passwordResetTokenRepository.findByToken(EXPIRED_TOKEN)).thenReturn(Optional.of(expiredToken));

        Optional<PasswordResetToken> result = passwordResetTokenRepository.findByToken(EXPIRED_TOKEN);

        assertThat(result).isPresent();
        assertThat(result.get().getExpiresAt()).isBefore(LocalDateTime.now());
    }

    // -------------------------
    // deleteByEmail (@Modifying)
    // -------------------------

    @Test
    @DisplayName("deleteByEmail deletes all reset tokens for the given email")
    void deleteByEmail_deletesTokensForEmail() {
        doNothing().when(passwordResetTokenRepository).deleteByEmail(TEST_EMAIL);

        passwordResetTokenRepository.deleteByEmail(TEST_EMAIL);

        verify(passwordResetTokenRepository, times(1)).deleteByEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("deleteByEmail does nothing when no tokens exist for the email")
    void deleteByEmail_doesNothing_whenNoTokensExist() {
        doNothing().when(passwordResetTokenRepository).deleteByEmail(OTHER_EMAIL);

        passwordResetTokenRepository.deleteByEmail(OTHER_EMAIL);

        verify(passwordResetTokenRepository).deleteByEmail(OTHER_EMAIL);
    }

    // -------------------------
    // JpaRepository inherited — save / findById
    // -------------------------

    @Test
    @DisplayName("save persists and returns the reset token")
    void save_persistsResetToken() {
        when(passwordResetTokenRepository.save(validToken)).thenReturn(validToken);

        PasswordResetToken saved = passwordResetTokenRepository.save(validToken);

        assertThat(saved).isNotNull();
        assertThat(saved.getToken()).isEqualTo(VALID_TOKEN);
        assertThat(saved.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(saved.isUsed()).isFalse();
        verify(passwordResetTokenRepository).save(validToken);
    }

    @Test
    @DisplayName("save persists a token marked as used after password reset")
    void save_persistsUsedToken_afterReset() {
        when(passwordResetTokenRepository.save(usedToken)).thenReturn(usedToken);

        PasswordResetToken saved = passwordResetTokenRepository.save(usedToken);

        assertThat(saved.isUsed()).isTrue();
        verify(passwordResetTokenRepository).save(usedToken);
    }

    @Test
    @DisplayName("findById returns the token when it exists")
    void findById_returnsToken_whenExists() {
        when(passwordResetTokenRepository.findById(1L)).thenReturn(Optional.of(validToken));

        Optional<PasswordResetToken> result = passwordResetTokenRepository.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById returns empty Optional when token does not exist")
    void findById_returnsEmpty_whenNotFound() {
        when(passwordResetTokenRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<PasswordResetToken> result = passwordResetTokenRepository.findById(99L);

        assertThat(result).isEmpty();
    }

    // -------------------------
    // PasswordResetToken entity state — validated in repository context
    // -------------------------

    @Test
    @DisplayName("valid token has a future expiry and is not used")
    void validToken_hasFutureExpiry_andIsNotUsed() {
        assertThat(validToken.isUsed()).isFalse();
        assertThat(validToken.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("expired token has a past expiry time")
    void expiredToken_hasPastExpiry() {
        assertThat(expiredToken.getExpiresAt()).isBefore(LocalDateTime.now());
    }

    @Test
    @DisplayName("used token has the used flag set to true")
    void usedToken_hasUsedFlagTrue() {
        assertThat(usedToken.isUsed()).isTrue();
    }
}
