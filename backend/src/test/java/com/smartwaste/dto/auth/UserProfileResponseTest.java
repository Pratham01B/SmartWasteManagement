package com.smartwaste.dto.auth;

import com.smartwaste.entity.Role;
import com.smartwaste.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserProfileResponse.
 * Verifies that the static factory method correctly maps User fields.
 */
class UserProfileResponseTest {

    // -------------------------------------------------------------------------
    // Helper: build a fully-populated User
    // -------------------------------------------------------------------------
    private User buildUser() {
        LocalDateTime now = LocalDateTime.of(2024, 6, 1, 10, 0);
        return User.builder()
                .id(42L)
                .firstName("Ravi")
                .lastName("Kumar")
                .email("ravi.kumar@example.com")
                .passwordHash("hashed_password")
                .phoneNumber("9876543210")
                .role(Role.CITIZEN)
                .city("Mumbai")
                .pincode("400001")
                .rewardPoints(150)
                .createdAt(now)
                .build();
    }

    // -------------------------------------------------------------------------
    // from(User) — happy path
    // -------------------------------------------------------------------------

    @Test
    void from_mapsAllFieldsCorrectly() {
        User user = buildUser();

        UserProfileResponse response = UserProfileResponse.from(user);

        assertThat(response.getUserId()).isEqualTo(42L);
        assertThat(response.getEmail()).isEqualTo("ravi.kumar@example.com");
        assertThat(response.getFullName()).isEqualTo("Ravi Kumar");
        assertThat(response.getPhoneNumber()).isEqualTo("9876543210");
        assertThat(response.getCity()).isEqualTo("Mumbai");
        assertThat(response.getPincode()).isEqualTo("400001");
        assertThat(response.getRole()).isEqualTo(Role.CITIZEN);
        assertThat(response.getRewardPoints()).isEqualTo(150);
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 6, 1, 10, 0));
    }

    @Test
    void from_fullNameConcatenatesFirstAndLastName() {
        User user = User.builder()
                .id(1L)
                .firstName("Priya")
                .lastName("Sharma")
                .email("priya@example.com")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .rewardPoints(0)
                .build();

        UserProfileResponse response = UserProfileResponse.from(user);

        assertThat(response.getFullName()).isEqualTo("Priya Sharma");
    }

    @Test
    void from_zeroRewardPoints_mappedCorrectly() {
        User user = buildUser().toBuilder()
                .rewardPoints(0)
                .build();

        UserProfileResponse response = UserProfileResponse.from(user);

        assertThat(response.getRewardPoints()).isZero();
    }

    @Test
    void from_nullOptionalFields_mappedAsNull() {
        // phoneNumber, city, pincode are optional — verify nulls pass through
        User user = User.builder()
                .id(99L)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .passwordHash("hash")
                .role(Role.WORKER)
                .rewardPoints(0)
                .phoneNumber(null)
                .city(null)
                .pincode(null)
                .createdAt(null)
                .build();

        UserProfileResponse response = UserProfileResponse.from(user);

        assertThat(response.getPhoneNumber()).isNull();
        assertThat(response.getCity()).isNull();
        assertThat(response.getPincode()).isNull();
        assertThat(response.getCreatedAt()).isNull();
    }

    @Test
    void from_allRoles_mappedCorrectly() {
        for (Role role : Role.values()) {
            User user = User.builder()
                    .id(1L)
                    .firstName("A")
                    .lastName("B")
                    .email("a@b.com")
                    .passwordHash("hash")
                    .role(role)
                    .rewardPoints(0)
                    .build();

            UserProfileResponse response = UserProfileResponse.from(user);

            assertThat(response.getRole())
                    .as("Role %s should be mapped correctly", role)
                    .isEqualTo(role);
        }
    }

    // -------------------------------------------------------------------------
    // Builder / @Data sanity checks
    // -------------------------------------------------------------------------

    @Test
    void builderAndGetters_workCorrectly() {
        LocalDateTime ts = LocalDateTime.of(2025, 1, 15, 8, 30);
        UserProfileResponse response = UserProfileResponse.builder()
                .userId(7L)
                .email("worker@smartwaste.com")
                .fullName("Amit Singh")
                .phoneNumber("9000000001")
                .city("Delhi")
                .pincode("110001")
                .role(Role.WORKER)
                .rewardPoints(20)
                .createdAt(ts)
                .build();

        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getEmail()).isEqualTo("worker@smartwaste.com");
        assertThat(response.getFullName()).isEqualTo("Amit Singh");
        assertThat(response.getPhoneNumber()).isEqualTo("9000000001");
        assertThat(response.getCity()).isEqualTo("Delhi");
        assertThat(response.getPincode()).isEqualTo("110001");
        assertThat(response.getRole()).isEqualTo(Role.WORKER);
        assertThat(response.getRewardPoints()).isEqualTo(20);
        assertThat(response.getCreatedAt()).isEqualTo(ts);
    }
}
