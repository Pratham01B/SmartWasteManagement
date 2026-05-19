package com.smartwaste.service;

import com.smartwaste.dto.marketplace.ListingRequest;
import com.smartwaste.dto.marketplace.ListingResponse;
import com.smartwaste.entity.MarketplaceListing;
import com.smartwaste.entity.MarketplaceListing.ListingStatus;
import com.smartwaste.entity.MarketplaceListing.MaterialType;
import com.smartwaste.entity.User;
import com.smartwaste.exception.ResourceNotFoundException;
import com.smartwaste.repository.MarketplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for the waste marketplace module.
 */
@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final MarketplaceRepository marketplaceRepository;

    /** Browse all active listings (public). */
    public Page<ListingResponse> getActiveListings(Pageable pageable) {
        return marketplaceRepository.findByStatus(ListingStatus.ACTIVE, pageable)
                .map(ListingResponse::from);
    }

    /** Browse active listings filtered by material type. */
    public Page<ListingResponse> getListingsByMaterial(MaterialType materialType, Pageable pageable) {
        return marketplaceRepository.findByMaterialTypeAndStatus(materialType, ListingStatus.ACTIVE, pageable)
                .map(ListingResponse::from);
    }

    /** Get listings posted by the current user. */
    public Page<ListingResponse> getMyListings(User seller, Pageable pageable) {
        return marketplaceRepository.findBySeller(seller, pageable)
                .map(ListingResponse::from);
    }

    /** Get a single listing by ID. */
    public ListingResponse getById(Long id) {
        return ListingResponse.from(findOrThrow(id));
    }

    /** Create a new listing. */
    @Transactional
    public ListingResponse createListing(ListingRequest request, User seller) {
        MarketplaceListing listing = MarketplaceListing.builder()
                .seller(seller)
                .title(request.getTitle())
                .description(request.getDescription())
                .materialType(request.getMaterialType())
                .quantityKg(request.getQuantityKg())
                .pricePerKg(request.getPricePerKg())
                .city(request.getCity())
                .pincode(request.getPincode())
                .imageUrl(request.getImageUrl())
                .build();

        return ListingResponse.from(marketplaceRepository.save(listing));
    }

    /** Update an existing listing (only by the seller). */
    @Transactional
    public ListingResponse updateListing(Long id, ListingRequest request, User seller) {
        MarketplaceListing listing = findOrThrow(id);

        if (!listing.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("You can only edit your own listings");
        }

        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setMaterialType(request.getMaterialType());
        listing.setQuantityKg(request.getQuantityKg());
        listing.setPricePerKg(request.getPricePerKg());
        listing.setCity(request.getCity());
        listing.setPincode(request.getPincode());
        listing.setImageUrl(request.getImageUrl());

        return ListingResponse.from(marketplaceRepository.save(listing));
    }

    /** Mark a listing as sold or cancelled. */
    @Transactional
    public ListingResponse updateStatus(Long id, ListingStatus newStatus, User user) {
        MarketplaceListing listing = findOrThrow(id);

        if (!listing.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("You can only update your own listings");
        }

        listing.setStatus(newStatus);
        return ListingResponse.from(marketplaceRepository.save(listing));
    }

    /** Delete a listing (seller or admin). */
    @Transactional
    public void deleteListing(Long id, User user) {
        MarketplaceListing listing = findOrThrow(id);

        boolean isOwner = listing.getSeller().getId().equals(user.getId());
        boolean isAdmin = user.getRole().name().equals("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You can only delete your own listings");
        }

        marketplaceRepository.delete(listing);
    }

    // ── helpers ──────────────────────────────────────────

    private MarketplaceListing findOrThrow(Long id) {
        return marketplaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + id));
    }
}
