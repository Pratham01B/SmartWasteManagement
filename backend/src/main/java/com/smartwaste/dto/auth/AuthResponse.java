package com.smartwaste.dto.auth;

import com.smartwaste.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after successful login or registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    // User info embedded so the frontend doesn't need a separate /me call
    private Long userId;
    private String email;
    private String fullName;
    private Role role;
    private Integer rewardPoints;
}
