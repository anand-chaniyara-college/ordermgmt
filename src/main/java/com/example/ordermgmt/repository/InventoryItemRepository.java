package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    @Query("SELECT i FROM InventoryItem i WHERE i.availableStock > 0 AND i.pricingCatalog.unitPrice IS NOT NULL")
    List<InventoryItem> findAvailableWithPricing();

    @Query("SELECT i FROM InventoryItem i WHERE i.availableStock > 0 AND i.pricingCatalog.unitPrice IS NOT NULL")
    Page<InventoryItem> findAvailableWithPricing(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.itemId IN :itemIds ORDER BY i.itemId")
    List<InventoryItem> findAllByItemIdInForUpdate(@Param("itemIds") List<UUID> itemIds);
}
