package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.PricingCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingCatalogRepository extends JpaRepository<PricingCatalog, String> {

    // Finds the current/latest price for an item
    Optional<PricingCatalog> findFirstByInventoryItemItemIdOrderByCreatedTimestampDesc(String itemId);

    // Finds full price history for an item
    List<PricingCatalog> findAllByInventoryItemItemIdOrderByCreatedTimestampDesc(String itemId);
}
