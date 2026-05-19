package com.smartwaste.repository;

import com.smartwaste.entity.MarketplaceListing;
import com.smartwaste.entity.MarketplaceListing.ListingStatus;
import com.smartwaste.entity.MarketplaceListing.MaterialType;
import com.smartwaste.entity.Role;
import com.smartwaste.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MarketplaceRepository.
 * Uses Mockito to verify query method contracts without a live database.
 */
@ExtendWith(MockitoExtension.class)
class MarketplaceRepositoryTest {

    @Mock
    private MarketplaceRepository marketplaceRepository;

    private User seller;
    private MarketplaceListing activePlasticListing;
    private MarketplaceListing soldMetalListing;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        seller = User.builder()
                .id(1L)
                .firstName("Rahul")
                .lastName("Sharma")
                .email("rahul@example.com")
                .passwordHash("hashed")
                .role(Role.RECYCLER)
                .build();

        activePlasticListing = MarketplaceListing.builder()
                .id(1L)
                .seller(seller)
                .title("Plastic Bottles")
                .materialType(MaterialType.PLASTIC)
                .quantityKg(50.0)
                .pricePerKg(5.0)
                .status(ListingStatus.ACTIVE)
                .build();

        soldMetalListing = MarketplaceListing.builder()
                .id(2L)
                .seller(seller)
                .title("Scrap Metal")
                .materialType(MaterialType.METAL)
                .quantityKg(100.0)
                .pricePerKg(20.0)
                .status(ListingStatus.SOLD)
                .build();

        pageable = PageRequest.of(0, 10);
    }

    // ─────────────────────────────────────────────────────────────
    // findByStatus
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStatus returns listings matching the given status")
    void findByStatus_returnsMatchingListings() {
        Page<MarketplaceListing> expected = new PageImpl<>(List.of(activePlasticListing));
        when(marketplaceRepository.findByStatus(ListingStatus.ACTIVE, pageable)).thenReturn(expected);

        Page<MarketplaceListing> result = marketplaceRepository.findByStatus(ListingStatus.ACTIVE, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ListingStatus.ACTIVE);
        verify(marketplaceRepository, times(1)).findByStatus(ListingStatus.ACTIVE, pageable);
    }

    @Test
    @DisplayName("findByStatus returns empty page when no listings match")
    void findByStatus_returnsEmptyPage_whenNoMatch() {
        Page<MarketplaceListing> empty = Page.empty(pageable);
        when(marketplaceRepository.findByStatus(ListingStatus.EXPIRED, pageable)).thenReturn(empty);

        Page<MarketplaceListing> result = marketplaceRepository.findByStatus(ListingStatus.EXPIRED, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        verify(marketplaceRepository).findByStatus(ListingStatus.EXPIRED, pageable);
    }

    // ─────────────────────────────────────────────────────────────
    // findBySeller
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findBySeller returns all listings for the given seller")
    void findBySeller_returnsSellerListings() {
        Page<MarketplaceListing> expected = new PageImpl<>(List.of(activePlasticListing, soldMetalListing));
        when(marketplaceRepository.findBySeller(seller, pageable)).thenReturn(expected);

        Page<MarketplaceListing> result = marketplaceRepository.findBySeller(seller, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(l -> l.getSeller().getId().equals(seller.getId()));
        verify(marketplaceRepository).findBySeller(seller, pageable);
    }

    @Test
    @DisplayName("findBySeller returns empty page for seller with no listings")
    void findBySeller_returnsEmptyPage_whenSellerHasNoListings() {
        User newSeller = User.builder().id(99L).firstName("New").lastName("User")
                .email("new@example.com").passwordHash("hash").role(Role.RECYCLER).build();
        when(marketplaceRepository.findBySeller(newSeller, pageable)).thenReturn(Page.empty(pageable));

        Page<MarketplaceListing> result = marketplaceRepository.findBySeller(newSeller, pageable);

        assertThat(result.getTotalElements()).isZero();
        verify(marketplaceRepository).findBySeller(newSeller, pageable);
    }

    // ─────────────────────────────────────────────────────────────
    // findByMaterialTypeAndStatus
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByMaterialTypeAndStatus returns listings matching both material type and status")
    void findByMaterialTypeAndStatus_returnsMatchingListings() {
        Page<MarketplaceListing> expected = new PageImpl<>(List.of(activePlasticListing));
        when(marketplaceRepository.findByMaterialTypeAndStatus(MaterialType.PLASTIC, ListingStatus.ACTIVE, pageable))
                .thenReturn(expected);

        Page<MarketplaceListing> result = marketplaceRepository
                .findByMaterialTypeAndStatus(MaterialType.PLASTIC, ListingStatus.ACTIVE, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        MarketplaceListing listing = result.getContent().get(0);
        assertThat(listing.getMaterialType()).isEqualTo(MaterialType.PLASTIC);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        verify(marketplaceRepository).findByMaterialTypeAndStatus(MaterialType.PLASTIC, ListingStatus.ACTIVE, pageable);
    }

    @Test
    @DisplayName("findByMaterialTypeAndStatus returns empty page when no listings match both criteria")
    void findByMaterialTypeAndStatus_returnsEmptyPage_whenNoMatch() {
        when(marketplaceRepository.findByMaterialTypeAndStatus(MaterialType.GLASS, ListingStatus.ACTIVE, pageable))
                .thenReturn(Page.empty(pageable));

        Page<MarketplaceListing> result = marketplaceRepository
                .findByMaterialTypeAndStatus(MaterialType.GLASS, ListingStatus.ACTIVE, pageable);

        assertThat(result.getTotalElements()).isZero();
        verify(marketplaceRepository).findByMaterialTypeAndStatus(MaterialType.GLASS, ListingStatus.ACTIVE, pageable);
    }

    // ─────────────────────────────────────────────────────────────
    // countBySeller
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("countBySeller returns correct count for a seller with listings")
    void countBySeller_returnsCorrectCount() {
        when(marketplaceRepository.countBySeller(seller)).thenReturn(2L);

        long count = marketplaceRepository.countBySeller(seller);

        assertThat(count).isEqualTo(2L);
        verify(marketplaceRepository).countBySeller(seller);
    }

    @Test
    @DisplayName("countBySeller returns zero for a seller with no listings")
    void countBySeller_returnsZero_whenSellerHasNoListings() {
        User emptySeller = User.builder().id(50L).firstName("Empty").lastName("Seller")
                .email("empty@example.com").passwordHash("hash").role(Role.RECYCLER).build();
        when(marketplaceRepository.countBySeller(emptySeller)).thenReturn(0L);

        long count = marketplaceRepository.countBySeller(emptySeller);

        assertThat(count).isZero();
        verify(marketplaceRepository).countBySeller(emptySeller);
    }
}
