package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.example.ordermgmt.dto.AddStockRequestDTO;

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
    @Operation(summary = "View Inventory (Merged)", description = "Get inventory items. Returns specific item if itemId provided, paginated result if page/size provided, or full list otherwise.")
    public ResponseEntity<?> getInventory(
            @Parameter(description = "Specific Item ID to retrieve") @RequestParam(required = false) String itemId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size") @RequestParam(required = false) Integer size) {

        if (itemId != null && !itemId.isEmpty()) {
            logger.info("Processing getInventory for specific Item: {}", itemId);
            InventoryItemDTO item = inventoryService.getInventoryItem(itemId);
            logger.info("getInventory completed successfully for Item: {}", itemId);
            return ResponseEntity.ok(item);
        }

        if (page != null && size != null) {
            logger.info("Processing getInventory (Page) for Admin - Page: {}, Size: {}", page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<InventoryItemDTO> inventory = inventoryService.getAllInventory(pageable);
            logger.info("getInventory (Page) completed successfully for Admin");
            return ResponseEntity.ok(inventory);
        }

        logger.info("Processing getInventory (List) for Admin");
        List<InventoryItemDTO> inventory = inventoryService.getAllInventory();
        logger.info("getInventory (List) completed successfully for Admin");
        return ResponseEntity.ok(inventory);
    }

    @PostMapping
    @Operation(summary = "Add Inventory Items", description = "Add multiple inventory items in bulk")
    public ResponseEntity<List<String>> addInventoryItems(@Valid @RequestBody List<InventoryItemDTO> items) {
        logger.info("Processing addInventoryItems for {} items", items.size());
        List<String> result = inventoryService.addInventoryItems(items);
        logger.info("addInventoryItems completed successfully");
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(result);
    }

    @PostMapping("/addstock")
    @Operation(summary = "Add Stock", description = "Add stock to existing inventory items")
    public ResponseEntity<List<String>> addStock(
            @Valid @RequestBody List<AddStockRequestDTO> items) {
        logger.info("Processing addStock for {} items", items.size());
        List<String> result = inventoryService.addStock(items);
        logger.info("addStock completed successfully");
        return ResponseEntity.ok(result);
    }

    @PutMapping
    @Operation(summary = "Update Inventory Items", description = "Update multiple inventory items in bulk")
    public ResponseEntity<List<String>> updateInventoryItems(@Valid @RequestBody List<InventoryItemDTO> items) {
        logger.info("Processing updateInventoryItems for {} items", items.size());
        List<String> result = inventoryService.updateInventoryItems(items);
        logger.info("updateInventoryItems completed successfully");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{ids}")
    @Operation(summary = "Delete Inventory Items", description = "Delete multiple inventory items by ID (comma-separated)")
    public ResponseEntity<Void> deleteInventoryItems(@PathVariable List<String> ids) {
        logger.info("Processing deleteInventoryItems for IDs: {}", ids);
        inventoryService.deleteInventoryItems(ids);
        logger.info("deleteInventoryItems completed successfully");
        return ResponseEntity.noContent().build();
    }
}
