package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.CustomerProfileDTO;
import com.example.ordermgmt.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
    public ResponseEntity<CustomerProfileDTO> getProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Received request to get profile for customer: {}", email);
        return ResponseEntity.ok(customerService.getCustomerProfile(email));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update Profile Info", description = "Modify your name, phone number, or address")
    public ResponseEntity<String> updateProfile(@RequestBody CustomerProfileDTO profileDTO) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Received request to update profile for customer: {}", email);
        String result = customerService.updateCustomerProfile(email, profileDTO);
        return ResponseEntity.ok(result);
    }
}
