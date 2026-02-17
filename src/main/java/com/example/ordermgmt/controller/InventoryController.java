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
        logger.info("Processing getAllInventory for Admin");
        List<InventoryItemDTO> inventory = inventoryService.getAllInventory();
        logger.info("getAllInventory completed successfully for Admin");
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Check Stock for Item", description = "Check how many units of a specific product are currently in stock")
    public ResponseEntity<InventoryItemDTO> getInventoryItem(@PathVariable String itemId) {
        logger.info("Processing getInventoryItem for Item: {}", itemId);
        InventoryItemDTO item = inventoryService.getInventoryItem(itemId);
        logger.info("getInventoryItem completed successfully for Item: {}", itemId);
        return ResponseEntity.ok(item);
    }

    @PostMapping
    @Operation(summary = "Add New Product to Stock", description = "Register a brand new item in the inventory system")
    public ResponseEntity<String> addInventoryItem(@Valid @RequestBody InventoryItemDTO item) {
        logger.info("Processing addInventoryItem for Item: {}", item.getItemId());
        String result = inventoryService.addInventoryItem(item);
        logger.info("addInventoryItem completed successfully for Item: {}", item.getItemId());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Update Stock Quantity", description = "Report new stock arrivals or manual adjustments for an item")
    public ResponseEntity<String> updateInventoryItem(@PathVariable String itemId,
            @Valid @RequestBody InventoryItemDTO item) {
        logger.info("Processing updateInventoryItem for Item: {}", itemId);
        String result = inventoryService.updateInventoryItem(itemId, item);
        logger.info("updateInventoryItem completed successfully for Item: {}", itemId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "Remove Product from System", description = "Completely remove an item from being tracked in the inventory")
    public ResponseEntity<Void> deleteInventoryItem(@PathVariable String itemId) {
        logger.info("Processing deleteInventoryItem for Item: {}", itemId);
        inventoryService.deleteInventoryItem(itemId);
        logger.info("deleteInventoryItem completed successfully for Item: {}", itemId);
        return ResponseEntity.noContent().build();
    }
}
