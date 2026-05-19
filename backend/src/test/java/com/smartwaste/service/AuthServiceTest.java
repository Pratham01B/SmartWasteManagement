package com.smartwaste.service;

import com.smartwaste.dto.auth.AuthResponse;
import com.smartwaste.dto.auth.LoginRequest;
import com.smartwaste.dto.auth.RegisterRequest;
import com.smartwaste.entity.OtpToken;
import com.smartwaste.entity.PasswordResetToken;
import com.smartwaste.entity.Role;
import com.smartwaste.entity.User;
import com.smartwaste.exception.EmailAlreadyExistsException;
import com.smartwaste.exception.ResourceNotFoundException;
import com.smartwaste.repository.OtpRepository;
import com.smartwaste.repository.PasswordResetTokenRepository;
import com.smartwaste.repository.UserRepository;
import com.smartwaste.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private OtpRepository otpRepository;

    @InjectMocks
    private AuthService authService;

    // -------------------------
    // Shared test fixtures
    // -------------------------

    private User buildUser(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .passwordHash("hashed_password")
                .role(role)
                .rewardPoints(0)
                .isActive(true)
                .isEmailVerified(false)
                .build();
    }

    private RegisterRequest buildRegisterRequest(String email, Role role) {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail(email);
        req.setPassword("password123");
        req.setRole(role);
        req.setPhoneNumber("9876543210");
        req.setAddress("123 Main St");
        req.setCity("Bhopal");
        req.setPincode("462001");
        return req;
    }

    // =========================================================
    // register()
    // =========================================================

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should register a new user and return tokens with user info")
        void register_success() {
            RegisterRequest request = buildRegisterRequest("john@example.com", Role.CITIZEN);
            User savedUser = buildUser(1L, "john@example.com", Role.CITIZEN);

            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("access_token");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh_token");

            AuthResponse response = authService.register(request);

            assertThat(response.getAccessToken()).isEqualTo("access_token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getFullName()).isEqualTo("John Doe");
            assertThat(response.getRole()).isEqualTo(Role.CITIZEN);
            assertThat(response.getRewardPoints()).isEqualTo(0);
        }

        @Test
        @DisplayName("should save user with encoded password, not plain text")
        void register_encodesPassword() {
            RegisterRequest request = buildRegisterRequest("john@example.com", Role.CITIZEN);
            User savedUser = buildUser(1L, "john@example.com", Role.CITIZEN);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed_password");
        }

        @Test
        @DisplayName("should throw EmailAlreadyExistsException when email is taken")
        void register_duplicateEmail_throwsException() {
            RegisterRequest request = buildRegisterRequest("taken@example.com", Role.CITIZEN);
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("taken@example.com");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should store null phone number when blank string is provided")
        void register_blankPhoneNumber_storedAsNull() {
            RegisterRequest request = buildRegisterRequest("john@example.com", Role.CITIZEN);
            request.setPhoneNumber("   "); // blank
            User savedUser = buildUser(1L, "john@example.com", Role.CITIZEN);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPhoneNumber()).isNull();
        }

        @Test
        @DisplayName("should store null phone number when null is provided")
        void register_nullPhoneNumber_storedAsNull() {
            RegisterRequest request = buildRegisterRequest("john@example.com", Role.CITIZEN);
            request.setPhoneNumber(null);
            User savedUser = buildUser(1L, "john@example.com", Role.CITIZEN);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPhoneNumber()).isNull();
        }

        @Test
        @DisplayName("should generate both access and refresh tokens on successful registration")
        void register_generatesBothTokens() {
            RegisterRequest request = buildRegisterRequest("john@example.com", Role.CITIZEN);
            User savedUser = buildUser(1L, "john@example.com", Role.CITIZEN);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenReturn(savedUser);
            when(jwtService.generateToken(any())).thenReturn("access_token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");

            authService.register(request);

            verify(jwtService).generateToken(savedUser);
            verify(jwtService).generateRefreshToken(savedUser);
        }
    }

    // =========================================================
    // login()
    // =========================================================

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("should authenticate and return tokens with user info")
        void login_success() {
            LoginRequest request = new LoginRequest();
            request.setEmail("john@example.com");
            request.setPassword("password123");

            User user = buildUser(1L, "john@example.com", Role.CITIZEN);

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("access_token");
            when(jwtService.generateRefreshToken(user)).thenReturn("refresh_token");

            AuthResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access_token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getRole()).isEqualTo(Role.CITIZEN);
        }

        @Test
        @DisplayName("should delegate authentication to AuthenticationManager")
        void login_delegatesToAuthenticationManager() {
            LoginRequest request = new LoginRequest();
            request.setEmail("john@example.com");
            request.setPassword("password123");

            User user = buildUser(1L, "john@example.com", Role.CITIZEN);
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(jwtService.generateToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

            authService.login(request);

            verify(authenticationManager).authenticate(
                    new UsernamePasswordAuthenticationToken("john@example.com", "password123")
            );
        }

        @Test
        @DisplayName("should propagate BadCredentialsException on wrong password")
        void login_wrongPassword_throwsBadCredentials() {
            LoginRequest request = new LoginRequest();
            request.setEmail("john@example.com");
            request.setPassword("wrong_password");

            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("should throw RuntimeException when user not found after authentication")
        void login_userNotFoundAfterAuth_throwsRuntimeException() {
            LoginRequest request = new LoginRequest();
            request.setEmail("ghost@example.com");
            request.setPassword("password123");

            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found after authentication");
        }
    }

    // =========================================================
    // refreshToken()
    // =========================================================

    @Nested
    @DisplayName("refreshToken()")
    class RefreshToken {

        @Test
        @DisplayName("should issue a new access token for a valid refresh token")
        void refreshToken_success() {
            String refreshToken = "valid_refresh_token";
            User user = buildUser(1L, "john@example.com", Role.CITIZEN);

            when(jwtService.extractUsername(refreshToken)).thenReturn("john@example.com");
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
            when(jwtService.isTokenValid(refreshToken, user)).thenReturn(true);
            when(jwtService.generateToken(user)).thenReturn("new_access_token");

            AuthResponse response = authService.refreshToken(refreshToken);

            assertThat(response.getAccessToken()).isEqualTo("new_access_token");
            // Refresh token itself is passed through unchanged
            assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
        }

        @Test
        @DisplayName("should throw RuntimeException when refresh token is invalid")
        void refreshToken_invalidToken_throwsRuntimeException() {
            String refreshToken = "expired_token";
            User user = buildUser(1L, "john@example.com", Role.CITIZEN);

            when(jwtService.extractUsername(refreshToken)).thenReturn("john@example.com");
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
            when(jwtService.isTokenValid(refreshToken, user)).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        @DisplayName("should throw RuntimeException when user not found for refresh token")
        void refreshToken_userNotFound_throwsRuntimeException() {
            String refreshToken = "some_token";

            when(jwtService.extractUsername(refreshToken)).thenReturn("ghost@example.com");
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should not generate a new refresh token — reuses the existing one")
        void refreshToken_doesNotRotateRefreshToken() {
            String refreshToken = "valid_refresh_token";
            User user = buildUser(1L, "john@example.com", Role.CITIZEN);

            when(jwtService.extractUsername(refreshToken)).thenReturn("john@example.com");
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
            when(jwtService.isTokenValid(refreshToken, user)).thenReturn(true);
            when(jwtService.generateToken(user)).thenReturn("new_access_token");

            authService.refreshToken(refreshToken);

            verify(jwtService, never()).generateRefreshToken(any());
        }
    }

    // =========================================================
    // forgotPassword()
    // =========================================================

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPassword {

        @Test
        @DisplayName("should delete old token, save new token, and send reset link email")
        void forgotPassword_success() {
            String email = "john@example.com";
            User user = buildUser(1L, email, Role.CITIZEN);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            authService.forgotPassword(email);

            // Old token must be purged first via passwordResetTokenRepository
            verify(tokenRepository).deleteByEmail(email);

            // A new token must be persisted
            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            PasswordResetToken saved = tokenCaptor.getValue();
            assertThat(saved.getEmail()).isEqualTo(email);
            assertThat(saved.getToken()).isNotBlank();
            assertThat(saved.isUsed()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

            // Reset email must be sent with a full reset link containing the token
            ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendPasswordResetLink(eq(email), linkCaptor.capture());
            assertThat(linkCaptor.getValue()).contains(saved.getToken());
            assertThat(linkCaptor.getValue()).contains("reset-password");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when email is not registered")
        void forgotPassword_unknownEmail_throwsResourceNotFound() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.forgotPassword("ghost@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(tokenRepository, never()).save(any());
            verify(emailService, never()).sendPasswordResetLink(anyString(), anyString());
        }

        @Test
        @DisplayName("should set token expiry approximately 15 minutes in the future")
        void forgotPassword_tokenExpiresIn15Minutes() {
            String email = "john@example.com";
            User user = buildUser(1L, email, Role.CITIZEN);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            LocalDateTime before = LocalDateTime.now().plusMinutes(14);
            authService.forgotPassword(email);
            LocalDateTime after = LocalDateTime.now().plusMinutes(16);

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());
            LocalDateTime expiry = captor.getValue().getExpiresAt();
            assertThat(expiry).isAfter(before).isBefore(after);
        }

        @Test
        @DisplayName("should generate a unique UUID token each time")
        void forgotPassword_generatesUniqueToken() {
            User user = buildUser(1L, "john@example.com", Role.CITIZEN);
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

            authService.forgotPassword("john@example.com");
            authService.forgotPassword("john@example.com");

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository, times(2)).save(captor.capture());
            List<PasswordResetToken> saved = captor.getAllValues();
            assertThat(saved.get(0).getToken()).isNotEqualTo(saved.get(1).getToken());
        }
    }

    // =========================================================
    // resetPassword()
    // =========================================================

    @Nested
    @DisplayName("resetPassword()")
    class ResetPassword {

        private PasswordResetToken validToken(String email) {
            return PasswordResetToken.builder()
                    .id(1L)
                    .token("valid-token-uuid")
                    .email(email)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .used(false)
                    .build();
        }

        @Test
        @DisplayName("should update password and mark token as used on success")
        void resetPassword_success() {
            String email = "john@example.com";
            User user = buildUser(1L, email, Role.CITIZEN);
            PasswordResetToken token = validToken(email);

            when(tokenRepository.findByToken("valid-token-uuid")).thenReturn(Optional.of(token));
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newPass123")).thenReturn("hashed_new");

            authService.resetPassword("valid-token-uuid", "newPass123");

            // Password must be updated with encoded value
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed_new");

            // Token must be marked used
            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().isUsed()).isTrue();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for unknown token")
        void resetPassword_unknownToken_throwsResourceNotFound() {
            when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword("bad-token", "newPass"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when token is expired")
        void resetPassword_expiredToken_throwsResourceNotFound() {
            PasswordResetToken expired = PasswordResetToken.builder()
                    .id(2L)
                    .token("expired-token")
                    .email("john@example.com")
                    .expiresAt(LocalDateTime.now().minusMinutes(1)) // already past
                    .used(false)
                    .build();

            when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> authService.resetPassword("expired-token", "newPass"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("expired");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when token was already used")
        void resetPassword_alreadyUsedToken_throwsResourceNotFound() {
            PasswordResetToken used = PasswordResetToken.builder()
                    .id(3L)
                    .token("used-token")
                    .email("john@example.com")
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .used(true) // already consumed
                    .build();

            when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(used));

            assertThatThrownBy(() -> authService.resetPassword("used-token", "newPass"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("already used");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user linked to token no longer exists")
        void resetPassword_userNotFound_throwsResourceNotFound() {
            PasswordResetToken token = validToken("deleted@example.com");

            when(tokenRepository.findByToken("valid-token-uuid")).thenReturn(Optional.of(token));
            when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword("valid-token-uuid", "newPass"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================
    // sendOtp()
    // =========================================================

    @Nested
    @DisplayName("sendOtp()")
    class SendOtp {

        @Test
        @DisplayName("should delete old OTPs, save a new OTP token, and send email")
        void sendOtp_success() {
            String email = "john@example.com";

            authService.sendOtp(email);

            // Old OTPs for this email must be purged first
            verify(otpRepository).deleteByEmail(email);

            // A new OTP token must be persisted
            ArgumentCaptor<OtpToken> captor = ArgumentCaptor.forClass(OtpToken.class);
            verify(otpRepository).save(captor.capture());
            OtpToken saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo(email);
            assertThat(saved.getOtp()).hasSize(6);
            assertThat(saved.isUsed()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

            // Email must be sent with the generated OTP
            verify(emailService).sendOtp(eq(email), eq(saved.getOtp()));
        }

        @Test
        @DisplayName("should generate a 6-digit numeric OTP")
        void sendOtp_generates6DigitOtp() {
            authService.sendOtp("john@example.com");

            ArgumentCaptor<OtpToken> captor = ArgumentCaptor.forClass(OtpToken.class);
            verify(otpRepository).save(captor.capture());
            String otp = captor.getValue().getOtp();

            assertThat(otp).matches("\\d{6}");
        }

        @Test
        @DisplayName("should set OTP expiry approximately 5 minutes in the future")
        void sendOtp_expiresIn5Minutes() {
            LocalDateTime before = LocalDateTime.now().plusMinutes(4);
            authService.sendOtp("john@example.com");
            LocalDateTime after = LocalDateTime.now().plusMinutes(6);

            ArgumentCaptor<OtpToken> captor = ArgumentCaptor.forClass(OtpToken.class);
            verify(otpRepository).save(captor.capture());
            LocalDateTime expiry = captor.getValue().getExpiresAt();
            assertThat(expiry).isAfter(before).isBefore(after);
        }

        @Test
        @DisplayName("should always delete old OTPs before saving a new one")
        void sendOtp_deletesOldOtpBeforeSaving() {
            authService.sendOtp("john@example.com");

            // deleteByEmail must be called before save
            var inOrder = inOrder(otpRepository);
            inOrder.verify(otpRepository).deleteByEmail("john@example.com");
            inOrder.verify(otpRepository).save(any(OtpToken.class));
        }
    }

    // =========================================================
    // verifyOtp()
    // =========================================================

    @Nested
    @DisplayName("verifyOtp()")
    class VerifyOtp {

        private OtpToken validOtpToken(String email, String otp) {
            return OtpToken.builder()
                    .id(1L)
                    .email(email)
                    .otp(otp)
                    .expiresAt(LocalDateTime.now().plusMinutes(3))
                    .used(false)
                    .build();
        }

        @Test
        @DisplayName("should return JWT token for an existing user on valid OTP")
        void verifyOtp_existingUser_returnsJwt() {
            String email = "john@example.com";
            String otp = "123456";
            User user = buildUser(1L, email, Role.CITIZEN);
            OtpToken token = validOtpToken(email, otp);

            when(otpRepository.findByEmailAndUsedFalse(email)).thenReturn(Optional.of(token));
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("jwt_token");

            String result = authService.verifyOtp(email, otp);

            assertThat(result).isEqualTo("jwt_token");
        }

        @Test
        @DisplayName("should create a new CITIZEN user when email is not yet registered")
        void verifyOtp_newUser_createsAndReturnsJwt() {
            String email = "newuser@example.com";
            String otp = "654321";
            OtpToken token = validOtpToken(email, otp);
            User createdUser = buildUser(2L, email, Role.CITIZEN);

            when(otpRepository.findByEmailAndUsedFalse(email)).thenReturn(Optional.of(token));
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("random_hash");
            when(userRepository.save(any(User.class))).thenReturn(createdUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("new_jwt");

            String result = authService.verifyOtp(email, otp);

            assertThat(result).isEqualTo("new_jwt");

            // Verify the new user is saved with CITIZEN role and verified email
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getRole()).isEqualTo(Role.CITIZEN);
            assertThat(saved.getIsEmailVerified()).isTrue();
            assertThat(saved.getEmail()).isEqualTo(email);
        }

        @Test
        @DisplayName("should mark OTP as used after successful verification")
        void verifyOtp_marksOtpAsUsed() {
            String email = "john@example.com";
            String otp = "111222";
            User user = buildUser(1L, email, Role.CITIZEN);
            OtpToken token = validOtpToken(email, otp);

            when(otpRepository.findByEmailAndUsedFalse(email)).thenReturn(Optional.of(token));
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(jwtService.generateToken(any())).thenReturn("jwt");

            authService.verifyOtp(email, otp);

            ArgumentCaptor<OtpToken> captor = ArgumentCaptor.forClass(OtpToken.class);
            verify(otpRepository).save(captor.capture());
            assertThat(captor.getValue().isUsed()).isTrue();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when no unused OTP exists for email")
        void verifyOtp_noOtpFound_throwsResourceNotFound() {
            when(otpRepository.findByEmailAndUsedFalse("ghost@example.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyOtp("ghost@example.com", "123456"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("OTP not found");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when OTP is expired")
        void verifyOtp_expiredOtp_throwsResourceNotFound() {
            String email = "john@example.com";
            OtpToken expired = OtpToken.builder()
                    .id(1L)
                    .email(email)
                    .otp("999999")
                    .expiresAt(LocalDateTime.now().minusMinutes(1)) // already past
                    .used(false)
                    .build();

            when(otpRepository.findByEmailAndUsedFalse(email)).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> authService.verifyOtp(email, "999999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when OTP value does not match")
        void verifyOtp_wrongOtp_throwsResourceNotFound() {
            String email = "john@example.com";
            OtpToken token = validOtpToken(email, "123456");

            when(otpRepository.findByEmailAndUsedFalse(email)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.verifyOtp(email, "000000"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Invalid OTP");
        }
    }
}
