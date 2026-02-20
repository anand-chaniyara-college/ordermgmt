package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.AdminPricingDTO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import com.example.ordermgmt.service.AdminPriceService;
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
    @Operation(summary = "View Prices (Merged)", description = "Get pricing. Returns specific item price if itemId provided, paginated result if page/size provided, or full list otherwise.")
    public ResponseEntity<?> getPrices(
            @Parameter(description = "Specific Item ID to retrieve price for") @RequestParam(required = false) String itemId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size") @RequestParam(required = false) Integer size) {

        if (itemId != null && !itemId.isEmpty()) {
            logger.info("Processing getPrice for specific Item: {}", itemId);
            AdminPricingDTO price = adminPriceService.getPrice(itemId);
            logger.info("getPrice completed successfully for Item: {}", itemId);
            return ResponseEntity.ok(price);
        }

        if (page != null && size != null) {
            logger.info("Processing getAllPrices (Page) for Admin - Page: {}, Size: {}", page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<AdminPricingDTO> prices = adminPriceService.getAllPrices(pageable);
            logger.info("getAllPrices (Page) completed successfully for Admin");
            return ResponseEntity.ok(prices);
        }

        logger.info("Processing getAllPrices (List) for Admin");
        List<AdminPricingDTO> prices = adminPriceService.getAllPrices();
        logger.info("getAllPrices (List) completed successfully for Admin");
        return ResponseEntity.ok(prices);
    }

    @PostMapping
    @Operation(summary = "Create Price Records", description = "Set initial prices for newly added products in bulk")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request format or parameters", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
public ResponseEntity<String> addPrices(@Valid @RequestBody List<AdminPricingDTO> pricingDTOs) {
        logger.info("Processing addPrices for {} items", pricingDTOs.size());
        adminPriceService.addPrices(pricingDTOs);
        logger.info("addPrices completed successfully");
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body("Price records added successfully.");
    }

    @PutMapping
    @Operation(summary = "Update Prices", description = "Adjust prices for products in bulk")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request format or parameters", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
public ResponseEntity<String> updatePrices(@Valid @RequestBody List<AdminPricingDTO> pricingDTOs) {
        logger.info("Processing updatePrices for {} items", pricingDTOs.size());
        adminPriceService.updatePrices(pricingDTOs);
        logger.info("updatePrices completed successfully");
        return ResponseEntity.ok("Prices updated successfully.");
    }
}
