package com.smartwaste.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Marketplace listing — recyclable material posted by a RECYCLER or CITIZEN.
 */
@Entity
@Table(name = "marketplace_listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @NotBlank
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "material_type", nullable = false, length = 30)
    private MaterialType materialType;

    @NotNull
    @Positive
    @Column(name = "quantity_kg", nullable = false)
    private Double quantityKg;

    @NotNull
    @Positive
    @Column(name = "price_per_kg", nullable = false)
    private Double pricePerKg;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "image_url")
    private String imageUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ListingStatus status = ListingStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum MaterialType {
        PLASTIC, PAPER, METAL, GLASS, ELECTRONIC, RUBBER, TEXTILE, OTHER
    }

    public enum ListingStatus {
        ACTIVE, SOLD, EXPIRED, CANCELLED
    }
}
