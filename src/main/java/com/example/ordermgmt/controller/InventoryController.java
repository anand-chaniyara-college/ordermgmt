package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryItemDTO>> getAllInventory() {
        logger.info("Received request to get all inventory items");
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<InventoryItemDTO> getInventoryItem(@PathVariable String itemId) {
        logger.info("Received request to get inventory item: {}", itemId);

        InventoryItemDTO item = inventoryService.getInventoryItem(itemId);

        if (item == null) {
            logger.warn("Inventory item not found: {}", itemId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @PostMapping
    public ResponseEntity<String> addInventoryItem(@RequestBody InventoryItemDTO item) {
        logger.info("Received request to add inventory item: {}", item.getItemId());

        String result = inventoryService.addInventoryItem(item);

        if ("Item ID already exists".equals(result)) {
            logger.warn("Add item failed: Item ID already exists - {}", item.getItemId());
            return ResponseEntity.badRequest().body(result);
        }

        logger.info("Inventory item added successfully: {}", item.getItemId());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<String> updateInventoryItem(@PathVariable String itemId, @RequestBody InventoryItemDTO item) {
        logger.info("Received request to update inventory item: {}", itemId);

        String result = inventoryService.updateInventoryItem(itemId, item);

        if ("Item not found".equals(result)) {
            logger.warn("Update item failed: Item not found - {}", itemId);
            return ResponseEntity.status(404).body(result);
        }

        logger.info("Inventory item updated successfully: {}", itemId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<String> deleteInventoryItem(@PathVariable String itemId) {
        logger.info("Received request to delete inventory item: {}", itemId);

        String result = inventoryService.deleteInventoryItem(itemId);

        if ("Item not found".equals(result)) {
            logger.warn("Delete item failed: Item not found - {}", itemId);
            return ResponseEntity.status(404).body(result);
        }

        logger.info("Inventory item deleted successfully: {}", itemId);
        return ResponseEntity.ok(result);
    }
}
