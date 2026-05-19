package com.smartwaste.controller;

import com.smartwaste.dto.marketplace.ListingRequest;
import com.smartwaste.dto.marketplace.ListingResponse;
import com.smartwaste.entity.MarketplaceListing.ListingStatus;
import com.smartwaste.entity.MarketplaceListing.MaterialType;
import com.smartwaste.entity.User;
import com.smartwaste.service.MarketplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Marketplace endpoints — buy/sell recyclable materials.
 */
@RestController
@RequestMapping("/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    /** GET /api/marketplace — browse all active listings */
    @GetMapping
    public ResponseEntity<Page<ListingResponse>> getListings(
            @RequestParam(required = false) MaterialType materialType,
            @PageableDefault(size = 12, sort = "createdAt") Pageable pageable) {

        if (materialType != null) {
            return ResponseEntity.ok(marketplaceService.getListingsByMaterial(materialType, pageable));
        }
        return ResponseEntity.ok(marketplaceService.getActiveListings(pageable));
    }

    /** GET /api/marketplace/{id} — single listing */
    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListing(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getById(id));
    }

    /** GET /api/marketplace/my — current user's listings */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('RECYCLER', 'CITIZEN', 'ADMIN')")
    public ResponseEntity<Page<ListingResponse>> getMyListings(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(marketplaceService.getMyListings(currentUser, pageable));
    }

    /** POST /api/marketplace — create a listing */
    @PostMapping
    @PreAuthorize("hasAnyRole('RECYCLER', 'CITIZEN', 'ADMIN')")
    public ResponseEntity<ListingResponse> createListing(
            @Valid @RequestBody ListingRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(marketplaceService.createListing(request, currentUser));
    }

    /** PUT /api/marketplace/{id} — update a listing */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECYCLER', 'CITIZEN', 'ADMIN')")
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody ListingRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(marketplaceService.updateListing(id, request, currentUser));
    }

    /** PATCH /api/marketplace/{id}/status — mark sold/cancelled */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECYCLER', 'CITIZEN', 'ADMIN')")
    public ResponseEntity<ListingResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ListingStatus status,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(marketplaceService.updateStatus(id, status, currentUser));
    }

    /** DELETE /api/marketplace/{id} */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECYCLER', 'CITIZEN', 'ADMIN')")
    public ResponseEntity<Void> deleteListing(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        marketplaceService.deleteListing(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
