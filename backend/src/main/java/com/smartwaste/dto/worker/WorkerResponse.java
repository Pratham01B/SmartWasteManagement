package com.smartwaste.dto.worker;

import com.smartwaste.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkerResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String city;
    private String pincode;
    private Boolean isActive;

    public static WorkerResponse from(User user) {
        return WorkerResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .city(user.getCity())
                .pincode(user.getPincode())
                .isActive(user.getIsActive())
                .build();
    }
}
