package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.PricingCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PricingCatalogRepository extends JpaRepository<PricingCatalog, PricingCatalog.PricingCatalogId> {

    // Corrected naming: Since createdTimestamp is inside the 'id'
    // (PricingCatalogId),
    // we must reference it as 'IdCreatedTimestamp'
    Optional<PricingCatalog> findFirstByIdItemIdOrderByIdCreatedTimestampDesc(String itemId);
}
