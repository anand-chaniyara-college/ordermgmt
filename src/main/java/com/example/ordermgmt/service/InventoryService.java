package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.InventoryItemDTO;
import java.util.List;

public interface InventoryService {
    List<InventoryItemDTO> getAllInventory();

    String addInventoryItem(InventoryItemDTO item);

    String updateInventoryItem(String itemId, InventoryItemDTO item);

    InventoryItemDTO getInventoryItem(String itemId);

    String deleteInventoryItem(String itemId);
}
