package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RefreshTokenRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenResponseDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.dto.RegistrationResponseDTO;
import com.example.ordermgmt.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDTO> register(@RequestBody RegistrationRequestDTO request) {
        logger.info("Received registration request for email: {}", request.getEmail());

        String result = authService.registerUser(request);

        if ("Email already exists".equals(result) || "Role not found".equals(result)) {
            logger.warn("Registration failed: {}", result);
            return ResponseEntity.badRequest().body(new RegistrationResponseDTO(result));
        }

        logger.info("Registration successful for email: {}", request.getEmail());
        return ResponseEntity.ok(new RegistrationResponseDTO(result));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        logger.info("Received login request for email: {}", request.getEmail());

        LoginResponseDTO response = authService.loginUser(request);

        if ("Login successful".equals(response.getMessage())) {
            logger.info("Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Login failed for email: {}: {}", request.getEmail(), response.getMessage());
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(@RequestBody RefreshTokenRequestDTO request) {
        logger.info("Received refresh token request");

        try {
            RefreshTokenResponseDTO response = authService.refreshToken(request);

            if (response.getAccessToken() == null) {
                logger.warn("Refresh token expired or invalid");
                return ResponseEntity.status(403).body(response);
            }

            logger.info("Token refreshed successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error during token refresh", e);
            return ResponseEntity.status(403).body(new RefreshTokenResponseDTO(null, null, null, e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody RefreshTokenRequestDTO request) {
        logger.info("Received logout request");
        authService.logoutUser(request);
        return ResponseEntity.ok("Logged out successfully");
    }
}
