package com.smartwaste.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Complaint entity — represents a waste-related complaint filed by a CITIZEN.
 */
@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    // Assigned worker (nullable until admin assigns)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_worker_id")
    private User assignedWorker;

    @NotBlank
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Enumerated(EnumType.STRING)
    @Column(name = "waste_type", length = 30)
    private WasteType wasteType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ComplaintStatus status = ComplaintStatus.PENDING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private Priority priority = Priority.MEDIUM;

    // URL to uploaded image (stored in Supabase Storage)
    @Column(name = "image_url")
    private String imageUrl;

    // Reward points awarded to citizen on resolution
    @Builder.Default
    @Column(name = "reward_points_awarded")
    private Integer rewardPointsAwarded = 0;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // -------------------------
    // Nested enums
    // -------------------------

    public enum ComplaintStatus {
        PENDING, ASSIGNED, IN_PROGRESS, RESOLVED, REJECTED
    }

    public enum WasteType {
        ORGANIC, PLASTIC, ELECTRONIC, HAZARDOUS, CONSTRUCTION, MIXED
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }
}
