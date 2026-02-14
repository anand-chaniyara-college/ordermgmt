package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RefreshTokenRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenResponseDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.RefreshToken;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import com.example.ordermgmt.security.JwtUtil;
import com.example.ordermgmt.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final com.example.ordermgmt.repository.CustomerRepository customerRepository;
    private final com.example.ordermgmt.repository.RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(AppUserRepository appUserRepository,
            UserRoleRepository userRoleRepository,
            com.example.ordermgmt.repository.CustomerRepository customerRepository,
            com.example.ordermgmt.repository.RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.customerRepository = customerRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String registerUser(RegistrationRequestDTO request) {
        logger.info("Processing registration for email: {}", request.getEmail());

        if (appUserRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Registration failed: Email already exists - {}", request.getEmail());
            return "Email already exists";
        }

        UserRole role = userRoleRepository.findByRoleName(request.getRoleName()).orElse(null);
        if (role == null) {
            logger.warn("Registration failed: Role not found - {}", request.getRoleName());
            return "Role not found";
        }

        AppUser newUser = new AppUser();
        newUser.setUserId(UUID.randomUUID().toString());
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(role);
        newUser.setIsActive(true);
        newUser.setCreatedTimestamp(LocalDateTime.now());

        appUserRepository.save(newUser);

        // Create an empty customer profile for CUSTOMER role
        if ("CUSTOMER".equalsIgnoreCase(request.getRoleName())) {
            com.example.ordermgmt.entity.Customer customer = new com.example.ordermgmt.entity.Customer();
            customer.setCustomerId(UUID.randomUUID().toString());
            customer.setAppUser(newUser);
            // Personal details will be updated later via profile update API
            customerRepository.save(customer);
            logger.info("Empty customer profile created for: {}", request.getEmail());
        }

        logger.info("User registered successfully: {}", request.getEmail());
        return "Registration successful";
    }

    @Override
    public LoginResponseDTO loginUser(LoginRequestDTO request) {
        logger.info("Processing login for email: {}", request.getEmail());

        AppUser user = appUserRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            logger.warn("Login failed: User not found - {}", request.getEmail());
            return new LoginResponseDTO(null, null, null, "User not found");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Login failed: Invalid credentials - {}", request.getEmail());
            return new LoginResponseDTO(null, null, null, "Invalid credentials");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            logger.warn("Login failed: User inactive - {}", request.getEmail());
            return new LoginResponseDTO(null, null, null, "User is inactive");
        }

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleName());
        RefreshToken refreshToken = createRefreshToken(user);

        logger.info("Login successful for user: {}", request.getEmail());

        return new LoginResponseDTO(accessToken, refreshToken.getToken(), user.getRole().getRoleName(),
                "Login successful");
    }

    @Override
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        logger.info("Processing refresh token request");

        String requestToken = request.getRefreshToken();

        return refreshTokenRepository.findByToken(requestToken)
                .map(token -> {
                    if (token.getExpiryDate().isBefore(java.time.Instant.now())) {
                        logger.warn("Refresh token expired");
                        return new RefreshTokenResponseDTO(null, null, "Bearer",
                                "Refresh token was expired. Please make a new signin request");
                    }

                    if (Boolean.TRUE.equals(token.getRevoked())) {
                        logger.warn("Refresh token is revoked");
                        throw new RuntimeException("Refresh token is revoked!");
                    }

                    AppUser user = token.getAppUser();
                    if (!Boolean.TRUE.equals(user.getIsActive())) {
                        logger.warn("User is inactive");
                        throw new RuntimeException("User is inactive!");
                    }

                    String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleName());

                    logger.info("Access token refreshed successfully for user: {}", user.getEmail());
                    return new RefreshTokenResponseDTO(newAccessToken, requestToken, "Bearer",
                            "Token refreshed successfully");
                })
                .orElseThrow(() -> {
                    logger.error("Refresh token not found in database");
                    return new RuntimeException("Refresh token is not in database!");
                });
    }

    private RefreshToken createRefreshToken(AppUser user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenId(UUID.randomUUID().toString());
        refreshToken.setAppUser(user);
        refreshToken.setExpiryDate(java.time.Instant.now().plusMillis(1000L * 60 * 60 * 24 * 30)); // 30 Days
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public String logoutUser(RefreshTokenRequestDTO request) {
        logger.info("Processing logout request");

        String requestToken = request.getRefreshToken();

        refreshTokenRepository.findByToken(requestToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            logger.info("Refresh token revoked");
        });

        return "Logout successful";
    }
}
