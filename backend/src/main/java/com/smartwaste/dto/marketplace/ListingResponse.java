package com.smartwaste.dto.marketplace;

import com.smartwaste.entity.MarketplaceListing;
import com.smartwaste.entity.MarketplaceListing.ListingStatus;
import com.smartwaste.entity.MarketplaceListing.MaterialType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ListingResponse {

    private Long id;
    private Long sellerId;
    private String sellerName;
    private String title;
    private String description;
    private MaterialType materialType;
    private Double quantityKg;
    private Double pricePerKg;
    private Double totalPrice;
    private String city;
    private String pincode;
    private String imageUrl;
    private ListingStatus status;
    private LocalDateTime createdAt;

    public static ListingResponse from(MarketplaceListing listing) {
        return ListingResponse.builder()
                .id(listing.getId())
                .sellerId(listing.getSeller().getId())
                .sellerName(listing.getSeller().getFullName())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .materialType(listing.getMaterialType())
                .quantityKg(listing.getQuantityKg())
                .pricePerKg(listing.getPricePerKg())
                .totalPrice(Math.round(listing.getQuantityKg() * listing.getPricePerKg() * 100.0) / 100.0)
                .city(listing.getCity())
                .pincode(listing.getPincode())
                .imageUrl(listing.getImageUrl())
                .status(listing.getStatus())
                .createdAt(listing.getCreatedAt())
                .build();
    }
}
