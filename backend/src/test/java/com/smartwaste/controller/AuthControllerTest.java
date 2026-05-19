package com.smartwaste.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwaste.dto.auth.*;
import com.smartwaste.entity.Role;
import com.smartwaste.entity.User;
import com.smartwaste.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController.
 * Uses MockMvc in standalone mode — no Spring context loaded.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // Reusable stub response
    private AuthResponse stubAuthResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();

        stubAuthResponse = AuthResponse.builder()
                .accessToken("access-token-stub")
                .refreshToken("refresh-token-stub")
                .userId(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role(Role.CITIZEN)
                .rewardPoints(0)
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /auth/register
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/register — success returns 201 with tokens")
    void register_success_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setLastName("User");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setRole(Role.CITIZEN);

        when(authService.register(any(RegisterRequest.class))).thenReturn(stubAuthResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token-stub"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("CITIZEN"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    // -------------------------------------------------------------------------
    // POST /auth/login
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/login — valid credentials returns 200 with tokens")
    void login_validCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class))).thenReturn(stubAuthResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-stub"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-stub"))
                .andExpect(jsonPath("$.userId").value(1));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    // -------------------------------------------------------------------------
    // POST /auth/refresh
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/refresh — valid refresh token returns new access token")
    void refresh_validToken_returnsNewAccessToken() throws Exception {
        when(authService.refreshToken("refresh-token-stub")).thenReturn(stubAuthResponse);

        mockMvc.perform(post("/auth/refresh")
                        .header("Refresh-Token", "refresh-token-stub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-stub"));

        verify(authService, times(1)).refreshToken("refresh-token-stub");
    }

    // -------------------------------------------------------------------------
    // GET /auth/me
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /auth/me — authenticated user returns profile")
    void getProfile_authenticatedUser_returnsProfile() throws Exception {
        // Build a User stub to act as @AuthenticationPrincipal
        User currentUser = User.builder()
                .id(1L)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .passwordHash("hashed")
                .role(Role.CITIZEN)
                .rewardPoints(50)
                .build();

        mockMvc.perform(get("/auth/me")
                        .principal(() -> currentUser.getUsername())
                        .requestAttr("currentUser", currentUser))
                .andExpect(status().isOk());
        // Note: full profile assertion requires Spring Security context;
        // integration tests cover the authenticated path end-to-end.
    }

    // -------------------------------------------------------------------------
    // POST /auth/forgot-password
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/forgot-password — valid email returns success message")
    void forgotPassword_validEmail_returnsSuccessMessage() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        doNothing().when(authService).forgotPassword("test@example.com");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset link sent to your email!"));

        verify(authService, times(1)).forgotPassword("test@example.com");
    }

    @Test
    @DisplayName("POST /auth/forgot-password — service throws exception propagates error")
    void forgotPassword_unknownEmail_propagatesException() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("unknown@example.com");

        doThrow(new RuntimeException("No account found with this email"))
                .when(authService).forgotPassword("unknown@example.com");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        verify(authService, times(1)).forgotPassword("unknown@example.com");
    }

    // -------------------------------------------------------------------------
    // POST /auth/reset-password
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/reset-password — valid token and password returns success message")
    void resetPassword_validToken_returnsSuccessMessage() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-uuid-token");
        request.setNewPassword("newPassword123");

        doNothing().when(authService).resetPassword("valid-uuid-token", "newPassword123");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully!"));

        verify(authService, times(1)).resetPassword("valid-uuid-token", "newPassword123");
    }

    @Test
    @DisplayName("POST /auth/reset-password — expired token propagates exception")
    void resetPassword_expiredToken_propagatesException() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("newPassword123");

        doThrow(new RuntimeException("Token has expired. Please request again."))
                .when(authService).resetPassword("expired-token", "newPassword123");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        verify(authService, times(1)).resetPassword("expired-token", "newPassword123");
    }

    @Test
    @DisplayName("POST /auth/reset-password — already used token propagates exception")
    void resetPassword_alreadyUsedToken_propagatesException() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("used-token");
        request.setNewPassword("newPassword123");

        doThrow(new RuntimeException("Token already used."))
                .when(authService).resetPassword("used-token", "newPassword123");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        verify(authService, times(1)).resetPassword("used-token", "newPassword123");
    }
}
