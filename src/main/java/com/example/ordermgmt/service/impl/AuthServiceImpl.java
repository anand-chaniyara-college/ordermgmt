package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RefreshTokenRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenResponseDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.RefreshToken;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.exception.AccountInactiveException;
import com.example.ordermgmt.exception.InvalidCredentialsException;
import com.example.ordermgmt.exception.InvalidTokenException;
import com.example.ordermgmt.exception.RoleNotFoundException;
import com.example.ordermgmt.exception.UserAlreadyExistsException;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.CustomerRepository;
import com.example.ordermgmt.repository.RefreshTokenRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import com.example.ordermgmt.entity.TokenBlacklist;
import com.example.ordermgmt.repository.TokenBlacklistRepository;
import com.example.ordermgmt.security.JwtUtil;
import com.example.ordermgmt.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final CustomerRepository customerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(AppUserRepository appUserRepository,
            UserRoleRepository userRoleRepository,
            CustomerRepository customerRepository,
            RefreshTokenRepository refreshTokenRepository,
            TokenBlacklistRepository tokenBlacklistRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.customerRepository = customerRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public void registerUser(RegistrationRequestDTO request) {
        logger.info("Processing registration for email: {}", request.getEmail());

        if (appUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        UserRole role = userRoleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + request.getRoleName()));

        AppUser newUser = new AppUser();
        newUser.setUserId(UUID.randomUUID().toString());
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(role);
        newUser.setIsActive(true);

        appUserRepository.save(newUser);

        if ("CUSTOMER".equalsIgnoreCase(request.getRoleName())) {
            createEmptyCustomerProfile(newUser);
        }

        logger.info("User registered successfully: {}", request.getEmail());
    }

    private void createEmptyCustomerProfile(AppUser user) {
        Customer customer = new Customer();
        customer.setCustomerId(UUID.randomUUID().toString());
        customer.setAppUser(user);
        customerRepository.save(customer);
        logger.info("Empty customer profile created for: {}", user.getEmail());
    }

    @Override
    public LoginResponseDTO loginUser(LoginRequestDTO request) {
        logger.info("Processing login for email: {}", request.getEmail());

        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AccountInactiveException("User is inactive");
        }

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleName());
        RefreshToken refreshToken = createRefreshToken(user);

        logger.info("Login successful for user: {}", request.getEmail());

        return new LoginResponseDTO(accessToken, refreshToken.getToken(), user.getRole().getRoleName(),
                "Login successful");
    }

    @Override
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request, String accessToken) {
        logger.info("Processing refresh token request");

        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        validateRefreshToken(token);

        // Blacklist existing access token
        blacklistAccessToken(accessToken);

        // Revoke old refresh token (Rotation)
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        AppUser user = token.getAppUser();
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AccountInactiveException("User is inactive");
        }

        // Issue new tokens
        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleName());
        RefreshToken newRefreshToken = createRefreshToken(user);

        logger.info("Access token refreshed and rotated successfully for user: {}", user.getEmail());
        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken.getToken(), "Bearer",
                "Token refreshed successfully");
    }

    private void blacklistAccessToken(String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            TokenBlacklist blacklistEntry = new TokenBlacklist();
            blacklistEntry.setToken(accessToken);

            blacklistEntry.setExpiryDate(Instant.now().plusSeconds(3600));
            tokenBlacklistRepository.save(blacklistEntry);
        }
    }

    private void validateRefreshToken(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token expired");
        }

        if (Boolean.TRUE.equals(token.getRevoked())) {
            throw new InvalidTokenException("Refresh token revoked");
        }
    }

    private RefreshToken createRefreshToken(AppUser user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenId(UUID.randomUUID().toString());
        refreshToken.setAppUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(1000L * 60 * 60 * 24 * 7));
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public void logoutUser(RefreshTokenRequestDTO request, String accessToken) {
        logger.info("Processing logout request");

        // Blacklist access token
        blacklistAccessToken(accessToken);

        // Revoke refresh token
        refreshTokenRepository.findByToken(request.getRefreshToken()).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            logger.info("Refresh token revoked");
        });
    }
}
