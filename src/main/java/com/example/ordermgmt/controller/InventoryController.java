package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.InventoryItemDTO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

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
import com.example.ordermgmt.dto.AddStockWrapperDTO;
import com.example.ordermgmt.dto.InventoryItemWrapperDTO;
import java.util.Map;
import java.util.List;
import java.util.UUID;

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
    @Operation(summary = "View Inventory", description = "Get inventory items. With itemId: returns single InventoryItemDTO. With page+size: returns paginated Page<InventoryItemDTO>. Otherwise: returns {\"inventory\": [...]}.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory retrieved successfully", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Item not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<?> getInventory(
            @Parameter(description = "Specific Item ID (UUID) to retrieve") @RequestParam(required = false) UUID itemId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size") @RequestParam(required = false) Integer size) {

        if (itemId != null) {
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
        return ResponseEntity.ok(Map.of("inventory", inventory));
    }

    @PostMapping
    @Operation(summary = "Add Inventory Items", description = "Add multiple inventory items in bulk. Request body: {\"inventory\": [{\"itemName\":\"...\",\"availableStock\":...},...]}. Response: {\"items\": [...]} (UUIDs auto-generated)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Items created — returns {\"items\": [...]}", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request format or parameters", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<?> addInventoryItems(@Valid @RequestBody InventoryItemWrapperDTO wrapper) {
        List<InventoryItemDTO> items = wrapper.getInventory();
        logger.info("Processing addInventoryItems for {} items", items.size());
        List<UUID> result = inventoryService.addInventoryItems(items);
        logger.info("addInventoryItems completed successfully");
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(Map.of("items", result));
    }

    @PostMapping("/addstock")
    @Operation(summary = "Add Stock", description = "Add stock to existing inventory items. Request body: {\"addstock\": [{\"itemId\":\"...\",\"addStock\":...},...]}. Response: {\"items\": [...]}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock added — returns {\"items\": [...]}", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request format or parameters", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<?> addStock(@Valid @RequestBody AddStockWrapperDTO wrapper) {
        List<AddStockRequestDTO> items = wrapper.getAddstock();
        logger.info("Processing addStock for {} items", items.size());
        List<UUID> result = inventoryService.addStock(items);
        logger.info("addStock completed successfully");
        return ResponseEntity.ok(Map.of("items", result));
    }

    @PutMapping
    @Operation(summary = "Update Inventory Items", description = "Update multiple inventory items in bulk. Request body: {\"inventory\": [...]}. Response: {\"items\": [...]}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Items updated — returns {\"items\": [...]}", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request format or parameters", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<?> updateInventoryItems(@Valid @RequestBody InventoryItemWrapperDTO wrapper) {
        List<InventoryItemDTO> items = wrapper.getInventory();
        logger.info("Processing updateInventoryItems for {} items", items.size());
        List<UUID> result = inventoryService.updateInventoryItems(items);
        logger.info("updateInventoryItems completed successfully");
        return ResponseEntity.ok(Map.of("items", result));
    }

    @DeleteMapping("/{ids}")
    @Operation(summary = "Delete Inventory Items", description = "Delete multiple inventory items by comma-separated UUIDs in path. Returns 204 No Content on success.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Items deleted successfully", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Item not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<Void> deleteInventoryItems(@PathVariable List<UUID> ids) {
        logger.info("Processing deleteInventoryItems for IDs: {}", ids);
        inventoryService.deleteInventoryItems(ids);
        logger.info("deleteInventoryItems completed successfully");
        return ResponseEntity.noContent().build();
    }
}
