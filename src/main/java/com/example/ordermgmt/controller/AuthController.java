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
import com.example.ordermgmt.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "0. Authentication", description = "Everything you need to sign up, log in, and stay secure")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int RATE_LIMIT_REQUESTS = 1;
    private static final int RATE_LIMIT_WINDOW_SECONDS = 60;

    private final AuthService authService;
    private final RateLimitingService rateLimitingService;

    public AuthController(AuthService authService, RateLimitingService rateLimitingService) {
        this.authService = authService;
        this.rateLimitingService = rateLimitingService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a New User", description = "Create a new account by providing your details and choosing a role (CUSTOMER or ADMIN). Rate-limited to 1 request per minute per IP.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = RegistrationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already exists", content = @Content),
            @ApiResponse(responseCode = "429", description = "Too many registration attempts — returns {\"message\": \"...\"}", content = @Content)
    })
    @SecurityRequirements() // No auth required
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequestDTO request,
            HttpServletRequest servletRequest) {
        String clientIp = getClientIp(servletRequest);

        if (!rateLimitingService.allowRequest(clientIp, RATE_LIMIT_REQUESTS, RATE_LIMIT_WINDOW_SECONDS)) {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(429)
                    .body(Map.of("message", "Too many registration attempts. Please try again later."));
        }

        logger.info("Processing register for User: {}", request.getEmail());
        authService.registerUser(request);
        logger.info("register completed successfully for User: {}", request.getEmail());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(new RegistrationResponseDTO("Registration successful"));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign In", description = "Log into your account using your email and password to get an access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    @SecurityRequirements() // No auth required
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        logger.info("Processing login for User: {}", request.getEmail());
        LoginResponseDTO response = authService.loginUser(request);
        logger.info("login completed successfully for User: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Security Token", description = "Extend your session by generating a new access token using your refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content(schema = @Schema(implementation = RefreshTokenResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token or unauthorized", content = @Content)
    })
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("Processing refreshToken for User");
        String accessToken = authHeader.replace(BEARER_PREFIX, "");
        RefreshTokenResponseDTO response = authService.refreshToken(request, accessToken);
        logger.info("refreshToken completed successfully for User");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Sign Out", description = "End your current session and invalidate your security token. Returns {\"message\": \"Logged out successfully\"}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully — returns {\"message\": \"...\"}", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequestDTO request,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("Processing logout for User");
        String accessToken = authHeader.replace(BEARER_PREFIX, "");
        authService.logoutUser(request, accessToken);
        logger.info("logout completed successfully for User");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
