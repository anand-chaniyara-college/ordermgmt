package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
@Tag(name = "6. Stock & Inventory (Admin)", description = "Check product availability, update quantity in hand, and manage the warehouse")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "View Full Inventory", description = "See the list of all products currently tracked in your warehouse")
    public ResponseEntity<List<InventoryItemDTO>> getAllInventory() {
        logger.info("Received request to get all inventory items");
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Check Stock for Item", description = "Check how many units of a specific product are currently in stock")
    public ResponseEntity<InventoryItemDTO> getInventoryItem(@PathVariable String itemId) {
        logger.info("Received request to get inventory item: {}", itemId);
        InventoryItemDTO item = inventoryService.getInventoryItem(itemId);
        return ResponseEntity.ok(item);
    }

    @PostMapping
    @Operation(summary = "Add New Product to Stock", description = "Register a brand new item in the inventory system")
    public ResponseEntity<String> addInventoryItem(@Valid @RequestBody InventoryItemDTO item) {
        logger.info("Received request to add inventory item: {}", item.getItemId());
        String result = inventoryService.addInventoryItem(item);
        logger.info("Inventory item added successfully: {}", item.getItemId());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Update Stock Quantity", description = "Report new stock arrivals or manual adjustments for an item")
    public ResponseEntity<String> updateInventoryItem(@PathVariable String itemId,
            @Valid @RequestBody InventoryItemDTO item) {
        logger.info("Received request to update inventory item: {}", itemId);
        String result = inventoryService.updateInventoryItem(itemId, item);
        logger.info("Inventory item updated successfully: {}", itemId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "Remove Product from System", description = "Completely remove an item from being tracked in the inventory")
    public ResponseEntity<String> deleteInventoryItem(@PathVariable String itemId) {
        logger.info("Received request to delete inventory item: {}", itemId);
        String result = inventoryService.deleteInventoryItem(itemId);
        logger.info("Inventory item deleted successfully: {}", itemId);
        return ResponseEntity.ok(result);
    }
}
