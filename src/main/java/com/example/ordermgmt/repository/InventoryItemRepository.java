package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {

    @Modifying
    @Query(value = "UPDATE ordermgmt.INVENTORY_ITEM SET itemid = :newItemId WHERE itemid = :oldItemId", nativeQuery = true)
    void updateItemId(@Param("oldItemId") String oldItemId, @Param("newItemId") String newItemId);

    @Query("SELECT i FROM InventoryItem i WHERE i.availableStock > 0 AND i.pricingCatalog.unitPrice IS NOT NULL")
    List<InventoryItem> findAvailableWithPricing();

    @Query("SELECT i FROM InventoryItem i WHERE i.availableStock > 0 AND i.pricingCatalog.unitPrice IS NOT NULL")
    Page<InventoryItem> findAvailableWithPricing(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.itemId = :itemId")
    Optional<InventoryItem> findByIdForUpdate(@Param("itemId") String itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.itemId IN :itemIds ORDER BY i.itemId")
    List<InventoryItem> findAllByItemIdInForUpdate(@Param("itemIds") List<String> itemIds);
}
