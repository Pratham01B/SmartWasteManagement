package com.smartwaste.dto.marketplace;

import com.smartwaste.entity.MarketplaceListing.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ListingRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Material type is required")
    private MaterialType materialType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Double quantityKg;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private Double pricePerKg;

    private String city;
    private String pincode;
    private String imageUrl;
}
