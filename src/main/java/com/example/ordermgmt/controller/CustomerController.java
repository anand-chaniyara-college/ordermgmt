package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.CustomerProfileDTO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import com.example.ordermgmt.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@Tag(name = "1. Customer Profile", description = "Manage your personal account information and settings")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get Profile Info", description = "Retrieve the name, email, and address of the currently logged-in user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request format or parameters", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
public ResponseEntity<CustomerProfileDTO> getProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Processing getProfile for Customer: {}", email);
        CustomerProfileDTO profile = customerService.getCustomerProfile(email);
        logger.info("getProfile completed successfully for Customer: {}", email);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update Profile Info", description = "Modify your name, phone number, or address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request format or parameters", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden access", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
public ResponseEntity<String> updateProfile(@Valid @RequestBody CustomerProfileDTO profileDTO) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Processing updateProfile for Customer: {}", email);
        String result = customerService.updateCustomerProfile(email, profileDTO);
        logger.info("updateProfile completed successfully for Customer: {}", email);
        return ResponseEntity.ok(result);
    }
}
