package com.smartwaste.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CollectionRoute entity — represents a daily waste collection route assigned to a worker.
 */
@Entity
@Table(name = "collection_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    @Column(name = "route_name", nullable = false, length = 100)
    private String routeName;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "area_name", length = 100)
    private String areaName;

    @Column(name = "pincode", length = 10)
    private String pincode;

    // Ordered list of stop coordinates stored as JSON string: "[{lat,lng,address},...]"
    @Column(name = "stops", columnDefinition = "TEXT")
    private String stops;

    // Total estimated distance in km
    @Column(name = "estimated_distance_km")
    private Double estimatedDistanceKm;

    // Estimated duration in minutes
    @Column(name = "estimated_duration_min")
    private Integer estimatedDurationMin;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RouteStatus status = RouteStatus.SCHEDULED;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum RouteStatus {
        SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
