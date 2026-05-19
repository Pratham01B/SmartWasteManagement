package com.smartwaste.controller;

import com.smartwaste.dto.auth.AuthResponse;
import com.smartwaste.dto.auth.LoginRequest;
import com.smartwaste.dto.auth.RegisterRequest;
import com.smartwaste.dto.auth.UserProfileResponse;
import com.smartwaste.entity.User;
import com.smartwaste.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.smartwaste.dto.auth.ForgotPasswordRequest;
import com.smartwaste.dto.auth.ResetPasswordRequest;

/**
 * Authentication endpoints — public, no JWT required.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Register a new user (citizen, worker, recycler, or admin).
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Authenticate and receive JWT tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/auth/refresh
     * Exchange a valid refresh token for a new access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    /**
     * GET /api/auth/me
     * Returns the current authenticated user's profile including latest reward points.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(UserProfileResponse.from(currentUser));
    }

    // Forgot Password
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(
            Map.of("message", "Password reset link sent to your email!")
        );
    }

    // Reset Password
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(
            request.getToken(), 
            request.getNewPassword()
        );
        return ResponseEntity.ok(
            Map.of("message", "Password reset successfully!")
        );
    }

    // OTP bhejo
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        authService.sendOtp(body.get("email"));
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    // OTP verify karo
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String token = authService.verifyOtp(
            body.get("email"),
            body.get("otp")
        );
        return ResponseEntity.ok(Map.of("token", token));
    }

    
}
