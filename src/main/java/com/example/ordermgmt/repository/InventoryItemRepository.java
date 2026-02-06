package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
    // This interface gives us standard CRUD operations for the Inventory table
    // Methods provided automatically: findAll(), findById(), save(), deleteById(),
    // etc.
}
