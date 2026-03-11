package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RefreshTokenRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenResponseDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.dto.RegistrationResponseDTO;
import com.example.ordermgmt.dto.ForgotPasswordRequestDTO;
import com.example.ordermgmt.dto.ResetPasswordRequestDTO;
import com.example.ordermgmt.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.rate-limit.auth.requests}")
    private int rateLimitRequests;

    @Value("${app.rate-limit.auth.window-seconds}")
    private int rateLimitWindowSeconds;

    private final AuthService authService;
    private final RateLimitingService rateLimitingService;

    public AuthController(AuthService authService, RateLimitingService rateLimitingService) {
        this.authService = authService;
        this.rateLimitingService = rateLimitingService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a New Customer", description = "Create a new CUSTOMER account for a specific organization subdomain. Rate-limited to 1 request per minute per IP.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = RegistrationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "403", description = "Only CUSTOMER registration allowed", content = @Content),
            @ApiResponse(responseCode = "404", description = "Organization not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate email or organization inactive", content = @Content),
            @ApiResponse(responseCode = "429", description = "Too many registration attempts — returns {\"message\": \"...\"}", content = @Content)
    })
    @SecurityRequirements()
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequestDTO request,
            HttpServletRequest servletRequest) {
        String clientIp = getClientIp(servletRequest);

        if (!rateLimitingService.allowRequest(clientIp, rateLimitRequests, rateLimitWindowSeconds)) {
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
    @Operation(summary = "Sign In", description = "Log into your account using organization subdomain, email, and password to get an access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    @SecurityRequirements()
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

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Request a temporary password to be sent to your email. Valid for 5 minutes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Temporary password sent", content = @Content),
            @ApiResponse(responseCode = "404", description = "User or organization not found", content = @Content),
            @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content)
    })
    @SecurityRequirements()
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request,
            HttpServletRequest servletRequest) {
        String clientIp = getClientIp(servletRequest);

        if (!rateLimitingService.allowRequest(clientIp, rateLimitRequests, rateLimitWindowSeconds)) {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(429)
                    .body(Map.of("message", "Too many requests. Please try again later."));
        }

        logger.info("Processing forgotPassword for User: {}", request.getEmail());
        authService.forgotPassword(request);
        logger.info("forgotPassword completed successfully for User: {}", request.getEmail());
        return ResponseEntity
                .ok(Map.of("message", "If an account matches, a temporary password has been sent to your email."));
    }

    @PatchMapping("/reset-password")
    @Operation(summary = "Reset Password", description = "Reset your password using the temporary password sent to your email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid or expired temporary password", content = @Content)
    })
    @SecurityRequirements()
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        logger.info("Processing resetPassword for User: {}", request.getEmail());
        authService.resetPassword(request);
        logger.info("resetPassword completed successfully for User: {}", request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset."));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
