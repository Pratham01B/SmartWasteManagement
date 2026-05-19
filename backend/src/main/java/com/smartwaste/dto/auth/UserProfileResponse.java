package com.smartwaste.dto.auth;

import com.smartwaste.entity.Role;
import com.smartwaste.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Lightweight profile DTO returned by GET /api/auth/me.
 * Used by the frontend to refresh reward points and user info.
 */
@Data
@Builder
public class UserProfileResponse {

    private Long userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String city;
    private String pincode;
    private Role role;
    private Integer rewardPoints;
    private LocalDateTime createdAt;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .city(user.getCity())
                .pincode(user.getPincode())
                .role(user.getRole())
                .rewardPoints(user.getRewardPoints())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
