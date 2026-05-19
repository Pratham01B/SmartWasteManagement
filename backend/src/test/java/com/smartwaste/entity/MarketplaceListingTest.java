package com.smartwaste.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the MarketplaceListing entity.
 * Covers builder defaults, enum values, and field assignments.
 */
class MarketplaceListingTest {

    // -------------------------
    // Builder default values
    // -------------------------

    @Test
    void builder_shouldDefaultStatusToActive() {
        MarketplaceListing listing = MarketplaceListing.builder()
                .title("Plastic Scrap")
                .materialType(MarketplaceListing.MaterialType.PLASTIC)
                .quantityKg(10.0)
                .pricePerKg(5.0)
                .seller(new User())
                .build();

        assertThat(listing.getStatus()).isEqualTo(MarketplaceListing.ListingStatus.ACTIVE);
    }

    // -------------------------
    // Builder field assignment
    // -------------------------

    @Test
    void builder_shouldSetAllProvidedFields() {
        User seller = new User();

        MarketplaceListing listing = MarketplaceListing.builder()
                .seller(seller)
                .title("Old Newspapers")
                .description("Bundle of old newspapers, dry and clean")
                .materialType(MarketplaceListing.MaterialType.PAPER)
                .quantityKg(25.5)
                .pricePerKg(3.0)
                .city("Bhopal")
                .pincode("462001")
                .imageUrl("https://example.com/paper.jpg")
                .status(MarketplaceListing.ListingStatus.SOLD)
                .build();

        assertThat(listing.getSeller()).isSameAs(seller);
        assertThat(listing.getTitle()).isEqualTo("Old Newspapers");
        assertThat(listing.getDescription()).isEqualTo("Bundle of old newspapers, dry and clean");
        assertThat(listing.getMaterialType()).isEqualTo(MarketplaceListing.MaterialType.PAPER);
        assertThat(listing.getQuantityKg()).isEqualTo(25.5);
        assertThat(listing.getPricePerKg()).isEqualTo(3.0);
        assertThat(listing.getCity()).isEqualTo("Bhopal");
        assertThat(listing.getPincode()).isEqualTo("462001");
        assertThat(listing.getImageUrl()).isEqualTo("https://example.com/paper.jpg");
        assertThat(listing.getStatus()).isEqualTo(MarketplaceListing.ListingStatus.SOLD);
    }

    // -------------------------
    // Setter behaviour
    // -------------------------

    @Test
    void setStatus_shouldUpdateStatus() {
        MarketplaceListing listing = MarketplaceListing.builder()
                .title("Metal Scrap")
                .materialType(MarketplaceListing.MaterialType.METAL)
                .quantityKg(50.0)
                .pricePerKg(12.0)
                .seller(new User())
                .build();

        listing.setStatus(MarketplaceListing.ListingStatus.EXPIRED);

        assertThat(listing.getStatus()).isEqualTo(MarketplaceListing.ListingStatus.EXPIRED);
    }

    @Test
    void setQuantityKg_shouldUpdateQuantity() {
        MarketplaceListing listing = MarketplaceListing.builder()
                .title("Glass Bottles")
                .materialType(MarketplaceListing.MaterialType.GLASS)
                .quantityKg(5.0)
                .pricePerKg(2.0)
                .seller(new User())
                .build();

        listing.setQuantityKg(8.5);

        assertThat(listing.getQuantityKg()).isEqualTo(8.5);
    }

    @Test
    void setPricePerKg_shouldUpdatePrice() {
        MarketplaceListing listing = MarketplaceListing.builder()
                .title("E-Waste")
                .materialType(MarketplaceListing.MaterialType.ELECTRONIC)
                .quantityKg(3.0)
                .pricePerKg(20.0)
                .seller(new User())
                .build();

        listing.setPricePerKg(25.0);

        assertThat(listing.getPricePerKg()).isEqualTo(25.0);
    }

    // -------------------------
    // MaterialType enum
    // -------------------------

    @Test
    void materialType_shouldContainAllExpectedValues() {
        assertThat(MarketplaceListing.MaterialType.values()).containsExactlyInAnyOrder(
                MarketplaceListing.MaterialType.PLASTIC,
                MarketplaceListing.MaterialType.PAPER,
                MarketplaceListing.MaterialType.METAL,
                MarketplaceListing.MaterialType.GLASS,
                MarketplaceListing.MaterialType.ELECTRONIC,
                MarketplaceListing.MaterialType.RUBBER,
                MarketplaceListing.MaterialType.TEXTILE,
                MarketplaceListing.MaterialType.OTHER
        );
    }

    // -------------------------
    // ListingStatus enum
    // -------------------------

    @Test
    void listingStatus_shouldContainAllExpectedValues() {
        assertThat(MarketplaceListing.ListingStatus.values()).containsExactlyInAnyOrder(
                MarketplaceListing.ListingStatus.ACTIVE,
                MarketplaceListing.ListingStatus.SOLD,
                MarketplaceListing.ListingStatus.EXPIRED,
                MarketplaceListing.ListingStatus.CANCELLED
        );
    }

    // -------------------------
    // No-args constructor
    // -------------------------

    @Test
    void noArgsConstructor_shouldCreateInstanceWithNullFields() {
        MarketplaceListing listing = new MarketplaceListing();

        assertThat(listing.getId()).isNull();
        assertThat(listing.getTitle()).isNull();
        assertThat(listing.getSeller()).isNull();
        assertThat(listing.getMaterialType()).isNull();
    }
}
