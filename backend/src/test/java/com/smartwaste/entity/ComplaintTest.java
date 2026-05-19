package com.smartwaste.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Complaint entity.
 * Covers builder defaults, enum values, and field assignments.
 */
class ComplaintTest {

    // -------------------------
    // Builder default values
    // -------------------------

    @Test
    void builder_shouldDefaultStatusToPending() {
        Complaint complaint = Complaint.builder()
                .title("Test")
                .address("123 Main St")
                .citizen(new User())
                .build();

        assertThat(complaint.getStatus()).isEqualTo(Complaint.ComplaintStatus.PENDING);
    }

    @Test
    void builder_shouldDefaultPriorityToMedium() {
        Complaint complaint = Complaint.builder()
                .title("Test")
                .address("123 Main St")
                .citizen(new User())
                .build();

        assertThat(complaint.getPriority()).isEqualTo(Complaint.Priority.MEDIUM);
    }

    @Test
    void builder_shouldDefaultRewardPointsAwardedToZero() {
        Complaint complaint = Complaint.builder()
                .title("Test")
                .address("123 Main St")
                .citizen(new User())
                .build();

        assertThat(complaint.getRewardPointsAwarded()).isEqualTo(0);
    }

    // -------------------------
    // Builder field assignment
    // -------------------------

    @Test
    void builder_shouldSetAllProvidedFields() {
        User citizen = new User();
        User worker = new User();

        Complaint complaint = Complaint.builder()
                .citizen(citizen)
                .assignedWorker(worker)
                .title("Garbage pile")
                .description("Large pile near bus stop")
                .address("MG Road, Bhopal")
                .latitude(23.2599)
                .longitude(77.4126)
                .pincode("462001")
                .wasteType(Complaint.WasteType.PLASTIC)
                .priority(Complaint.Priority.HIGH)
                .status(Complaint.ComplaintStatus.ASSIGNED)
                .imageUrl("https://example.com/img.jpg")
                .rewardPointsAwarded(10)
                .build();

        assertThat(complaint.getCitizen()).isSameAs(citizen);
        assertThat(complaint.getAssignedWorker()).isSameAs(worker);
        assertThat(complaint.getTitle()).isEqualTo("Garbage pile");
        assertThat(complaint.getDescription()).isEqualTo("Large pile near bus stop");
        assertThat(complaint.getAddress()).isEqualTo("MG Road, Bhopal");
        assertThat(complaint.getLatitude()).isEqualTo(23.2599);
        assertThat(complaint.getLongitude()).isEqualTo(77.4126);
        assertThat(complaint.getPincode()).isEqualTo("462001");
        assertThat(complaint.getWasteType()).isEqualTo(Complaint.WasteType.PLASTIC);
        assertThat(complaint.getPriority()).isEqualTo(Complaint.Priority.HIGH);
        assertThat(complaint.getStatus()).isEqualTo(Complaint.ComplaintStatus.ASSIGNED);
        assertThat(complaint.getImageUrl()).isEqualTo("https://example.com/img.jpg");
        assertThat(complaint.getRewardPointsAwarded()).isEqualTo(10);
    }

    // -------------------------
    // Setter behaviour
    // -------------------------

    @Test
    void setStatus_shouldUpdateStatus() {
        Complaint complaint = Complaint.builder()
                .title("Test")
                .address("Addr")
                .citizen(new User())
                .build();

        complaint.setStatus(Complaint.ComplaintStatus.IN_PROGRESS);

        assertThat(complaint.getStatus()).isEqualTo(Complaint.ComplaintStatus.IN_PROGRESS);
    }

    @Test
    void setResolvedAt_shouldPersistTimestamp() {
        Complaint complaint = Complaint.builder()
                .title("Test")
                .address("Addr")
                .citizen(new User())
                .build();

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        complaint.setResolvedAt(now);

        assertThat(complaint.getResolvedAt()).isEqualTo(now);
    }

    // -------------------------
    // ComplaintStatus enum
    // -------------------------

    @Test
    void complaintStatus_shouldContainAllExpectedValues() {
        assertThat(Complaint.ComplaintStatus.values()).containsExactlyInAnyOrder(
                Complaint.ComplaintStatus.PENDING,
                Complaint.ComplaintStatus.ASSIGNED,
                Complaint.ComplaintStatus.IN_PROGRESS,
                Complaint.ComplaintStatus.RESOLVED,
                Complaint.ComplaintStatus.REJECTED
        );
    }

    // -------------------------
    // WasteType enum
    // -------------------------

    @Test
    void wasteType_shouldContainAllExpectedValues() {
        assertThat(Complaint.WasteType.values()).containsExactlyInAnyOrder(
                Complaint.WasteType.ORGANIC,
                Complaint.WasteType.PLASTIC,
                Complaint.WasteType.ELECTRONIC,
                Complaint.WasteType.HAZARDOUS,
                Complaint.WasteType.CONSTRUCTION,
                Complaint.WasteType.MIXED
        );
    }

    // -------------------------
    // Priority enum
    // -------------------------

    @Test
    void priority_shouldContainAllExpectedValues() {
        assertThat(Complaint.Priority.values()).containsExactlyInAnyOrder(
                Complaint.Priority.LOW,
                Complaint.Priority.MEDIUM,
                Complaint.Priority.HIGH,
                Complaint.Priority.URGENT
        );
    }

    // -------------------------
    // No-args constructor
    // -------------------------

    @Test
    void noArgsConstructor_shouldCreateInstanceWithNullFields() {
        Complaint complaint = new Complaint();

        assertThat(complaint.getId()).isNull();
        assertThat(complaint.getTitle()).isNull();
        assertThat(complaint.getAddress()).isNull();
    }
}
