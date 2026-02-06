package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // Get all inventory items
    // Endpoint: GET /api/admin/inventory
    @GetMapping
    public ResponseEntity<List<InventoryItemDTO>> getAllInventory() {
        System.out.println(">>> [InventoryController] Received request to get all inventory");

        // Call Service to get the list
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    // Get single inventory item
    // Endpoint: GET /api/admin/inventory/{itemId}
    @GetMapping("/{itemId}")
    public ResponseEntity<InventoryItemDTO> getInventoryItem(@PathVariable String itemId) {
        System.out.println(">>> [InventoryController] Received request to get item: " + itemId);

        InventoryItemDTO item = inventoryService.getInventoryItem(itemId);

        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    // Add a new inventory item
    // Endpoint: POST /api/admin/inventory
    // Requires JSON body: { "itemId": "...", "availableStock": 10, ... }
    @PostMapping
    public ResponseEntity<String> addInventoryItem(@RequestBody InventoryItemDTO item) {
        System.out.println(">>> [InventoryController] Received request to add item: " + item.getItemId());

        String result = inventoryService.addInventoryItem(item);

        if ("Item ID already exists".equals(result)) {
            // Return 400 Bad Request
            return ResponseEntity.badRequest().body(result);
        }
        // Return 200 OK
        return ResponseEntity.ok(result);
    }

    // Update an inventory item
    // Endpoint: PUT /api/admin/inventory/{itemId}
    @PutMapping("/{itemId}")
    public ResponseEntity<String> updateInventoryItem(@PathVariable String itemId, @RequestBody InventoryItemDTO item) {
        System.out.println(">>> [InventoryController] Received request to update item: " + itemId);

        String result = inventoryService.updateInventoryItem(itemId, item);

        if ("Item not found".equals(result)) {
            // Return 404 Not Found
            return ResponseEntity.status(404).body(result);
        }
        return ResponseEntity.ok(result);
    }

    // Delete an inventory item
    // Endpoint: DELETE /api/admin/inventory/{itemId}
    @DeleteMapping("/{itemId}")
    public ResponseEntity<String> deleteInventoryItem(@PathVariable String itemId) {
        System.out.println(">>> [InventoryController] Received request to delete item: " + itemId);

        String result = inventoryService.deleteInventoryItem(itemId);

        if ("Item not found".equals(result)) {
            return ResponseEntity.status(404).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
