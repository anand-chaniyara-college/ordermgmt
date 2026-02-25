package com.example.ordermgmt.service.impl;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.Optional;
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
import com.example.ordermgmt.service.TokenBlacklistService;
import com.example.ordermgmt.security.JwtUtil;
import com.example.ordermgmt.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final long REFRESH_TOKEN_VALIDITY_MS = 1000L * 60 * 60 * 24 * 7; // 7 days

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final CustomerRepository customerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(AppUserRepository appUserRepository,
            UserRoleRepository userRoleRepository,
            CustomerRepository customerRepository,
            RefreshTokenRepository refreshTokenRepository,
            TokenBlacklistService tokenBlacklistService,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.customerRepository = customerRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public void registerUser(RegistrationRequestDTO request) {
        logger.info("Processing registerUser for User: {}", request.getEmail());

        if (appUserRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Skipping registerUser for User: {} - Email already exists", request.getEmail());
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        UserRole role = userRoleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> {
                    logger.error("registerUser failed for User: {} - Role not found", request.getEmail());
                    return new RoleNotFoundException("Role not found: " + request.getRoleName());
                });

        AppUser newUser = new AppUser();
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(role);
        newUser.setIsActive(true);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                request.getEmail(), null, Collections.singletonList(new SimpleGrantedAuthority(role.getRoleName())));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            appUserRepository.save(newUser);

            if ("CUSTOMER".equalsIgnoreCase(request.getRoleName())) {
                createEmptyCustomerProfile(newUser);
            }
        } finally {
            SecurityContextHolder.clearContext();
        }

        logger.info("registerUser completed successfully for User: {}", request.getEmail());
    }

    private void createEmptyCustomerProfile(AppUser user) {
        Customer customer = new Customer();
        customer.setAppUser(user);
        customerRepository.save(customer);
        logger.info("Empty customer profile created for: {}", user.getEmail());
    }

    @Override
    @Transactional
    public LoginResponseDTO loginUser(LoginRequestDTO request) {
        logger.info("Processing loginUser for User: {}", request.getEmail());

        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Skipping loginUser for User: {} - Invalid credentials", request.getEmail());
                    return new InvalidCredentialsException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Skipping loginUser for User: {} - Invalid credentials", request.getEmail());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            logger.warn("Skipping loginUser for User: {} - Account inactive", request.getEmail());
            throw new AccountInactiveException("User is inactive");
        }

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleName());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getRoleName())));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        RefreshToken refreshToken;
        try {
            refreshToken = createRefreshToken(user);
        } finally {
            SecurityContextHolder.clearContext();
        }

        logger.info("loginUser completed successfully for User: {}", request.getEmail());

        return new LoginResponseDTO(accessToken, refreshToken.getToken(), user.getRole().getRoleName(),
                "Login successful");
    }

    @Override
    @Transactional
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request, String accessToken) {
        logger.info("Processing refreshToken for User");

        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> {
                    logger.error("refreshToken failed - Token not found");
                    return new InvalidTokenException("Refresh token not found");
                });

        validateRefreshToken(token);

        blacklistAccessToken(accessToken);

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        AppUser user = token.getAppUser();
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            logger.warn("Skipping refreshToken for User: {} - Account inactive", user.getEmail());
            throw new AccountInactiveException("User is inactive");
        }

        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleName());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getRoleName())));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        RefreshToken newRefreshToken;
        try {
            newRefreshToken = createRefreshToken(user);
        } finally {
            SecurityContextHolder.clearContext();
        }

        logger.info("refreshToken completed successfully for User: {}", user.getEmail());
        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken.getToken(), "Bearer",
                "Token refreshed successfully");
    }

    private void blacklistAccessToken(String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            tokenBlacklistService.blacklistToken(accessToken);
        }
    }

    private void validateRefreshToken(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            logger.warn("Skipping refreshToken - Token expired");
            throw new InvalidTokenException("Refresh token expired");
        }

        if (Boolean.TRUE.equals(token.getRevoked())) {
            logger.warn("Skipping refreshToken - Token revoked");
            throw new InvalidTokenException("Refresh token revoked");
        }
    }

    private RefreshToken createRefreshToken(AppUser user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenId(UUID.randomUUID().toString());
        refreshToken.setAppUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(REFRESH_TOKEN_VALIDITY_MS));
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void logoutUser(RefreshTokenRequestDTO request, String accessToken) {
        logger.info("Processing logoutUser for User");

        blacklistAccessToken(accessToken);

        refreshTokenRepository.findByToken(request.getRefreshToken()).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            logger.info("Refresh token revoked");
        });

        logger.info("logoutUser completed successfully");
    }
}
