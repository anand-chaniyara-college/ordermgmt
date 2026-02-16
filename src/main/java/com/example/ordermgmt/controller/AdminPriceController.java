package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.AdminPricingDTO;
import com.example.ordermgmt.service.AdminPriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/admin/prices")
@Tag(name = "5. Pricing Control (Admin)", description = "Adjust product prices, view historical price changes, and manage promotions")
public class AdminPriceController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPriceController.class);
    private final AdminPriceService adminPriceService;

    public AdminPriceController(AdminPriceService adminPriceService) {
        this.adminPriceService = adminPriceService;
    }

    @GetMapping
    @Operation(summary = "View Price List", description = "Get the current selling price and cost for all available products")
    public ResponseEntity<List<AdminPricingDTO>> getAllPrices() {
        logger.info("Received request to get all price records");
        return ResponseEntity.ok(adminPriceService.getAllPrices());
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Get Price Details", description = "Get the current price details for a specific item")
    public ResponseEntity<AdminPricingDTO> getPrice(@PathVariable String itemId) {
        logger.info("Received request to get price details for item: {}", itemId);
        return ResponseEntity.ok(adminPriceService.getPrice(itemId));
    }

    @PostMapping
    @Operation(summary = "Create New Price Record", description = "Set an initial price for a newly added product")
    public ResponseEntity<String> addPrice(@Valid @RequestBody AdminPricingDTO pricingDTO) {
        logger.info("Received request to add a new price for item: {}", pricingDTO.getItemId());
        adminPriceService.addPrice(pricingDTO);
        return ResponseEntity.ok("Price record added successfully.");
    }

    @PutMapping
    @Operation(summary = "Update Existing Price", description = "Adjust the current price of a product currently on sale")
    public ResponseEntity<String> updatePrice(@Valid @RequestBody AdminPricingDTO pricingDTO) {
        logger.info("Received request to update price for item: {}", pricingDTO.getItemId());
        adminPriceService.updatePrice(pricingDTO);
        return ResponseEntity.ok("Price updated successfully for item: " + pricingDTO.getItemId());
    }
}
