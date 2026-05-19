package com.smartwaste.dto.worker;

import com.smartwaste.entity.Role;
import com.smartwaste.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WorkerResponse DTO.
 * Verifies that the static factory method correctly maps User fields.
 */
class WorkerResponseTest {

    // -------------------------
    // Helper — builds a fully-populated User
    // -------------------------
    private User buildWorkerUser() {
        return User.builder()
                .id(42L)
                .firstName("Ravi")
                .lastName("Kumar")
                .email("ravi.kumar@example.com")
                .passwordHash("hashed_password")
                .phoneNumber("9876543210")
                .role(Role.WORKER)
                .city("Bhopal")
                .pincode("462001")
                .isActive(true)
                .build();
    }

    // -------------------------
    // from(User) — happy path
    // -------------------------

    @Test
    void from_shouldMapAllFieldsCorrectly() {
        User user = buildWorkerUser();

        WorkerResponse response = WorkerResponse.from(user);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getFullName()).isEqualTo("Ravi Kumar");
        assertThat(response.getEmail()).isEqualTo("ravi.kumar@example.com");
        assertThat(response.getPhoneNumber()).isEqualTo("9876543210");
        assertThat(response.getCity()).isEqualTo("Bhopal");
        assertThat(response.getPincode()).isEqualTo("462001");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void from_shouldConcatenateFirstAndLastName() {
        User user = User.builder()
                .id(1L)
                .firstName("Priya")
                .lastName("Sharma")
                .email("priya@example.com")
                .passwordHash("hash")
                .role(Role.WORKER)
                .build();

        WorkerResponse response = WorkerResponse.from(user);

        assertThat(response.getFullName()).isEqualTo("Priya Sharma");
    }

    @Test
    void from_shouldHandleNullOptionalFields() {
        // phoneNumber, city, pincode are optional — should map as null without throwing
        User user = User.builder()
                .id(10L)
                .firstName("Amit")
                .lastName("Singh")
                .email("amit@example.com")
                .passwordHash("hash")
                .role(Role.WORKER)
                .phoneNumber(null)
                .city(null)
                .pincode(null)
                .isActive(true)
                .build();

        WorkerResponse response = WorkerResponse.from(user);

        assertThat(response.getPhoneNumber()).isNull();
        assertThat(response.getCity()).isNull();
        assertThat(response.getPincode()).isNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("amit@example.com");
    }

    @Test
    void from_shouldReflectIsActiveFalse() {
        User user = buildWorkerUser();
        user.setIsActive(false);

        WorkerResponse response = WorkerResponse.from(user);

        assertThat(response.getIsActive()).isFalse();
    }

    // -------------------------
    // Builder — direct construction
    // -------------------------

    @Test
    void builder_shouldConstructResponseDirectly() {
        WorkerResponse response = WorkerResponse.builder()
                .id(99L)
                .fullName("Test Worker")
                .email("test@example.com")
                .phoneNumber("1234567890")
                .city("Indore")
                .pincode("452001")
                .isActive(true)
                .build();

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getFullName()).isEqualTo("Test Worker");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getPhoneNumber()).isEqualTo("1234567890");
        assertThat(response.getCity()).isEqualTo("Indore");
        assertThat(response.getPincode()).isEqualTo("452001");
        assertThat(response.getIsActive()).isTrue();
    }

    // -------------------------
    // Lombok @Data — equals / hashCode / toString
    // -------------------------

    @Test
    void equals_shouldReturnTrueForIdenticalResponses() {
        User user = buildWorkerUser();

        WorkerResponse r1 = WorkerResponse.from(user);
        WorkerResponse r2 = WorkerResponse.from(user);

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void toString_shouldContainEmailAndId() {
        WorkerResponse response = WorkerResponse.from(buildWorkerUser());

        String str = response.toString();

        assertThat(str).contains("ravi.kumar@example.com");
        assertThat(str).contains("42");
    }
}
