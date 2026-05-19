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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service handling user registration and login.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final OtpRepository otpRepository;

    /*
     * Registers a new user and returns JWT tokens.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(
                             (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) ? request.getPhoneNumber().trim() : null
                            )
                .role(request.getRole())
                .address(request.getAddress())
                .city(request.getCity())
                .pincode(request.getPincode())
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Authenticates an existing user and returns JWT tokens.
     */
    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException if invalid — Spring Security handles the 401
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Issues a new access token from a valid refresh token.
     */
    public AuthResponse refreshToken(String refreshToken) {
        String userEmail = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateToken(user);
        return buildAuthResponse(user, newAccessToken, refreshToken);
    }

    // -------------------------
    // Private helpers
    // -------------------------

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .rewardPoints(user.getRewardPoints())
                .build();
    }

    // ── OTP SEND ──────────────────────────────────────────
    public void sendOtp(String email) {
        // Purane OTP delete karo
        otpRepository.deleteByEmail(email);

        // Naya 6-digit OTP banao
        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);

        OtpToken token = OtpToken.builder()
                .email(email)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

        otpRepository.save(token);
        emailService.sendOtp(email, otp);
    }

    // ── OTP VERIFY ────────────────────────────────────────
    public String verifyOtp(String email, String otp) {
        Optional<OtpToken> tokenOpt = otpRepository.findByEmailAndUsedFalse(email);

        if (tokenOpt.isEmpty()) {
            throw new ResourceNotFoundException("OTP not found for email: " + email);
        }

        OtpToken token = tokenOpt.get();

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("OTP has expired");
        }

        if (!token.getOtp().equals(otp)) {
            throw new ResourceNotFoundException("Invalid OTP");
        }

        // OTP use hua mark karo
        token.setUsed(true);
        otpRepository.save(token);

        // User nahi hai toh create karo
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .role(Role.CITIZEN)
                            .isEmailVerified(true)
                            .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .firstName("User")
                            .lastName("")
                            .build();
                    return userRepository.save(newUser);
                });

        return jwtService.generateToken(user);
    }

     // ========================
     // Method 1 — Forgot Password
     // ========================

    // ── FORGOT PASSWORD ───────────────────────────────────
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Purana token delete karo
        passwordResetTokenRepository.deleteByEmail(email);

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email(email)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        emailService.sendPasswordResetLink(email, resetLink);
    }

    // ── RESET PASSWORD ────────────────────────────────────
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid reset token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Reset token has expired");
        }

        if (resetToken.isUsed()) {
            throw new ResourceNotFoundException("Reset token already used");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    
}
