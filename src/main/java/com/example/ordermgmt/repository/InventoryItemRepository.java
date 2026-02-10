package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {

    @Modifying
    @Query(value = "UPDATE ordermgmt.INVENTORY_ITEM SET itemid = :newItemId WHERE itemid = :oldItemId", nativeQuery = true)
    void updateItemId(@Param("oldItemId") String oldItemId, @Param("newItemId") String newItemId);
}
