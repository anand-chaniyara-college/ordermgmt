package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.AdminPricingDTO;
import com.example.ordermgmt.service.AdminPriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/prices")
public class AdminPriceController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPriceController.class);
    private final AdminPriceService adminPriceService;

    public AdminPriceController(AdminPriceService adminPriceService) {
        this.adminPriceService = adminPriceService;
    }

    @GetMapping
    public ResponseEntity<List<AdminPricingDTO>> getAllPrices() {
        logger.info("Admin request to get all price records");
        return ResponseEntity.ok(adminPriceService.getAllPrices());
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<List<AdminPricingDTO>> getPriceHistory(@PathVariable String itemId) {
        logger.info("Admin request to get price history for item: {}", itemId);
        return ResponseEntity.ok(adminPriceService.getPriceHistory(itemId));
    }

    @PostMapping
    public ResponseEntity<String> addPrice(@RequestBody AdminPricingDTO pricingDTO) {
        logger.info("Admin request to add a new price for item: {}", pricingDTO.getItemId());
        return ResponseEntity.ok(adminPriceService.addPrice(pricingDTO));
    }

    @PutMapping
    public ResponseEntity<String> updatePrice(@RequestBody AdminPricingDTO pricingDTO) {
        logger.info("Admin request to update existing price record");
        return ResponseEntity.ok(adminPriceService.updatePrice(pricingDTO));
    }
}
