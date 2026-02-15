package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RefreshTokenRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenResponseDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.dto.RegistrationResponseDTO;
import com.example.ordermgmt.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "0. Authentication", description = "Everything you need to sign up, log in, and stay secure")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a New User", description = "Create a new account by providing your details and choosing a role (CUSTOMER or ADMIN)")
    public ResponseEntity<RegistrationResponseDTO> register(@Valid @RequestBody RegistrationRequestDTO request) {
        logger.info("Received registration request for email: {}", request.getEmail());
        authService.registerUser(request);
        logger.info("Registration successful for email: {}", request.getEmail());
        return ResponseEntity.ok(new RegistrationResponseDTO("Registration successful"));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign In", description = "Log into your account using your email and password to get an access token")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        logger.info("Received login request for email: {}", request.getEmail());
        LoginResponseDTO response = authService.loginUser(request);
        logger.info("Login successful for email: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Security Token", description = "Extend your session by generating a new access token using your refresh token")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("Received refresh token request");
        String accessToken = authHeader.replace("Bearer ", "");
        RefreshTokenResponseDTO response = authService.refreshToken(request, accessToken);
        logger.info("Token refreshed successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Sign Out", description = "End your current session and invalidate your security token")
    public ResponseEntity<String> logout(@Valid @RequestBody RefreshTokenRequestDTO request,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("Received logout request");
        String accessToken = authHeader.replace("Bearer ", "");
        authService.logoutUser(request, accessToken);
        return ResponseEntity.ok("Logged out successfully");
    }
}
