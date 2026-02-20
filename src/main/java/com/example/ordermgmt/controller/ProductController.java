package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.ProductDTO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;

import com.example.ordermgmt.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/products")
@Tag(name = "3. Product Catalog", description = "Browse our full range of available products and prices")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Browse Products", description = "Get available products. Without pagination: returns {\"products\": [...]}. With page+size: returns paginated Page<ProductDTO>.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<?> getAvailableProducts(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size") @RequestParam(required = false) Integer size) {

        if (page != null && size != null) {
            logger.info("Processing getAvailableProducts (Page) for Customer - Page: {}, Size: {}", page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductDTO> products = productService.getAvailableProducts(pageable);
            logger.info("getAvailableProducts (Page) completed successfully for Customer");
            return ResponseEntity.ok(products);
        }

        logger.info("Processing getAvailableProducts for Customer");
        List<ProductDTO> products = productService.getAvailableProducts();
        logger.info("getAvailableProducts completed successfully for Customer");
        return ResponseEntity.ok(Map.of("products", products));
    }
}
