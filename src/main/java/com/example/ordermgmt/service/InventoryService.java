package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.InventoryItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.ordermgmt.dto.AddStockRequestDTO;

import java.util.List;
import java.util.UUID;

public interface InventoryService {
    List<InventoryItemDTO> getAllInventory();

    Page<InventoryItemDTO> getAllInventory(Pageable pageable);

    InventoryItemDTO getInventoryItem(UUID itemId);

    List<UUID> addInventoryItems(List<InventoryItemDTO> items);

    List<UUID> updateInventoryItems(List<InventoryItemDTO> items);

    List<UUID> addStock(List<AddStockRequestDTO> items);

    void deleteInventoryItems(List<UUID> itemIds);
}
