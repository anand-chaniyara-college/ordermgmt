package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.InventoryItemDTO;
import java.util.List;

public interface InventoryService {
    // Admin: Get all inventory items
    List<InventoryItemDTO> getAllInventory();

    // Admin: Add a new inventory item
    String addInventoryItem(InventoryItemDTO item);

    // Admin: Update an existing inventory item
    String updateInventoryItem(String itemId, InventoryItemDTO item);

    // Admin: Delete an inventory item
    String deleteInventoryItem(String itemId);
}
