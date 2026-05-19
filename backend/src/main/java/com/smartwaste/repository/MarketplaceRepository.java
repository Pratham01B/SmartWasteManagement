package com.smartwaste.repository;

import com.smartwaste.entity.MarketplaceListing;
import com.smartwaste.entity.MarketplaceListing.ListingStatus;
import com.smartwaste.entity.MarketplaceListing.MaterialType;
import com.smartwaste.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketplaceRepository extends JpaRepository<MarketplaceListing, Long> {

    Page<MarketplaceListing> findByStatus(ListingStatus status, Pageable pageable);

    Page<MarketplaceListing> findBySeller(User seller, Pageable pageable);

    Page<MarketplaceListing> findByMaterialTypeAndStatus(MaterialType materialType, ListingStatus status, Pageable pageable);

    long countBySeller(User seller);
}
