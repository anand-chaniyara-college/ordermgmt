package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.ProductDTO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import com.example.ordermgmt.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

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
    @Operation(summary = "Browse Products", description = "Get a list of all products currently available for purchase")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request format or parameters", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
public ResponseEntity<List<ProductDTO>> getAvailableProducts() {
        logger.info("Processing getAvailableProducts for Customer");
        List<ProductDTO> products = productService.getAvailableProducts();
        logger.info("getAvailableProducts completed successfully for Customer");
        return ResponseEntity.ok(products);
    }
}
