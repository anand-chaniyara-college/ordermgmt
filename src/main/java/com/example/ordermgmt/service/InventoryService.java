package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.InventoryItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.ordermgmt.dto.AddStockRequestDTO;

import java.util.List;

public interface InventoryService {
    List<InventoryItemDTO> getAllInventory();

    Page<InventoryItemDTO> getAllInventory(Pageable pageable);

    InventoryItemDTO getInventoryItem(String itemId);

    List<String> addInventoryItems(List<InventoryItemDTO> items);

    List<String> updateInventoryItems(List<InventoryItemDTO> items);

    List<String> addStock(List<AddStockRequestDTO> items);

    void deleteInventoryItems(List<String> itemIds);
}
