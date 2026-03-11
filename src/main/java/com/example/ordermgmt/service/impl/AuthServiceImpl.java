package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RefreshTokenRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenResponseDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.dto.ForgotPasswordRequestDTO;
import com.example.ordermgmt.dto.ResetPasswordRequestDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.Organization;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.exception.AccountInactiveException;
import com.example.ordermgmt.exception.InvalidCredentialsException;
import com.example.ordermgmt.exception.InvalidTokenException;
import com.example.ordermgmt.exception.OrganizationInactiveException;
import com.example.ordermgmt.exception.RegistrationForbiddenException;
import com.example.ordermgmt.exception.ResourceNotFoundException;
import com.example.ordermgmt.exception.RoleNotFoundException;
import com.example.ordermgmt.exception.UserAlreadyExistsException;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.CustomerRepository;
import com.example.ordermgmt.repository.OrganizationRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import com.example.ordermgmt.event.EmailDispatchEvent;
import com.example.ordermgmt.security.JwtUtil;
import com.example.ordermgmt.security.TenantContextHolder;
import com.example.ordermgmt.service.AuthService;
import com.example.ordermgmt.service.TokenBlacklistService;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String RT_PREFIX = "RT:";
    private static final String PR_PREFIX = "PR:";
    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final CustomerRepository customerRepository;
    private final OrganizationRepository organizationRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.jwt.refresh.expirationMs}")
    private long refreshExpirationMs;

    public AuthServiceImpl(AppUserRepository appUserRepository,
            UserRoleRepository userRoleRepository,
            CustomerRepository customerRepository,
            OrganizationRepository organizationRepository,
            TokenBlacklistService tokenBlacklistService,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            StringRedisTemplate redisTemplate,
            ApplicationEventPublisher eventPublisher) {
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.customerRepository = customerRepository;
        this.organizationRepository = organizationRepository;
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void registerUser(RegistrationRequestDTO request) {
        logger.info("Processing registerUser for User: {}", request.getEmail());

        if (!ROLE_CUSTOMER.equalsIgnoreCase(request.getRoleName())) {
            logger.warn("Skipping registerUser for User: {} - Public registration supports CUSTOMER only",
                    request.getEmail());
            throw new RegistrationForbiddenException("Only CUSTOMER registration is allowed on this endpoint");
        }

        UserRole role = userRoleRepository.findByRoleName(ROLE_CUSTOMER)
                .orElseThrow(() -> {
                    logger.error("registerUser failed for User: {} - Role not found", request.getEmail());
                    return new RoleNotFoundException("Role not found: " + ROLE_CUSTOMER);
                });

        Organization org = organizationRepository.findBySubdomainIgnoreCase(request.getOrgSubdomain())
                .orElseThrow(() -> {
                    logger.warn("Skipping registerUser for User: {} - Organization not found: {}",
                            request.getEmail(), request.getOrgSubdomain());
                    return new ResourceNotFoundException("Organization not found: " + request.getOrgSubdomain());
                });
        if (!Boolean.TRUE.equals(org.getIsActive())) {
            logger.warn("Skipping registerUser for User: {} - Organization inactive: {}",
                    request.getEmail(), request.getOrgSubdomain());
            throw new OrganizationInactiveException("Organization is inactive: " + request.getOrgSubdomain());
        }

        if (appUserRepository.existsByOrgIdAndEmailIgnoreCase(org.getOrgId(), request.getEmail())) {
            logger.warn("Skipping registerUser for User: {} - Email already exists in org: {}",
                    request.getEmail(), org.getOrgId());
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        AppUser newUser = new AppUser();
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(role);
        newUser.setIsActive(true);
        newUser.setOrgId(org.getOrgId());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                request.getEmail(), null,
                Collections.singletonList(new SimpleGrantedAuthority(role.getRoleName())));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        TenantContextHolder.setTenantId(org.getOrgId());
        try {
            appUserRepository.save(newUser);

            createEmptyCustomerProfile(newUser);
        } finally {
            TenantContextHolder.clear();
            SecurityContextHolder.clearContext();
        }

        logger.info("registerUser completed successfully for User: {}", request.getEmail());
    }

    private void createEmptyCustomerProfile(AppUser user) {
        Customer customer = new Customer();
        customer.setAppUser(user);
        customer.setOrgId(user.getOrgId());
        customerRepository.save(customer);
        logger.info("Empty customer profile created for: {}", user.getEmail());
    }

    @Override
    @Transactional
    public LoginResponseDTO loginUser(LoginRequestDTO request) {
        logger.info("Processing loginUser for User: {} in Org Subdomain: {}",
                request.getEmail(), request.getOrgSubdomain());

        Organization org = organizationRepository.findBySubdomainIgnoreCase(request.getOrgSubdomain())
                .orElseThrow(() -> {
                    logger.warn("Skipping loginUser for User: {} - Invalid credentials", request.getEmail());
                    return new InvalidCredentialsException("Invalid credentials");
                });

        if (!Boolean.TRUE.equals(org.getIsActive())) {
            logger.warn("Skipping loginUser for User: {} - Organization inactive: {}",
                    request.getEmail(), request.getOrgSubdomain());
            throw new OrganizationInactiveException("Organization is inactive: " + request.getOrgSubdomain());
        }

        AppUser user = appUserRepository.findByOrgIdAndEmailIgnoreCase(org.getOrgId(), request.getEmail())
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

        String accessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().getRoleName(),
                resolveTokenOrgId(user));
        String refreshToken = createRefreshToken(user.getUserId());

        logger.info("loginUser completed successfully for User: {}", request.getEmail());
        return new LoginResponseDTO(accessToken, refreshToken, user.getRole().getRoleName(), "Login successful");
    }

    @Override
    @Transactional
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request, String accessToken) {
        logger.info("Processing refreshToken");

        String storedUserId = redisTemplate.opsForValue().get(RT_PREFIX + request.getRefreshToken());
        if (storedUserId == null) {
            logger.warn("Skipping refreshToken - Token expired or not found");
            throw new InvalidTokenException("Refresh token expired or revoked");
        }

        redisTemplate.delete(RT_PREFIX + request.getRefreshToken());
        blacklistAccessToken(accessToken);

        UUID userId;
        try {
            userId = UUID.fromString(storedUserId);
        } catch (IllegalArgumentException ex) {
            logger.warn("Skipping refreshToken - Invalid token principal format");
            throw new InvalidTokenException("Invalid refresh token");
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("refreshToken failed - User not found for id: {}", userId);
                    return new InvalidTokenException("User not found for refresh token");
                });

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            logger.warn("Skipping refreshToken for User ID: {} - Account inactive", userId);
            throw new AccountInactiveException("User is inactive");
        }

        String newAccessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().getRoleName(),
                resolveTokenOrgId(user));
        String newRefreshToken = createRefreshToken(user.getUserId());

        logger.info("refreshToken completed successfully for User ID: {}", userId);
        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken, "Bearer", "Token refreshed successfully");
    }

    @Override
    @Transactional
    public void logoutUser(RefreshTokenRequestDTO request, String accessToken) {
        logger.info("Processing logoutUser");

        blacklistAccessToken(accessToken);

        String tokenValue = request.getRefreshToken();
        if (tokenValue != null && !tokenValue.isBlank()) {
            redisTemplate.delete(RT_PREFIX + tokenValue);
        }

        logger.info("logoutUser completed successfully");
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO request) {
        logger.info("Processing forgotPassword for User: {}", request.getEmail());

        // 1. Find User by email first to determine the tenant
        AppUser user = appUserRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> {
                    logger.warn("Skipping forgotPassword for User: {} - User not found", request.getEmail());
                    return new ResourceNotFoundException("User not found: " + request.getEmail());
                });

        // 2. Validate user belongs to the requested organization subdomain
        Organization org = organizationRepository.findBySubdomainIgnoreCase(request.getOrgSubdomain())
                .orElseThrow(() -> {
                    logger.warn("Skipping forgotPassword for User: {} - Organization not found: {}",
                            request.getEmail(), request.getOrgSubdomain());
                    return new ResourceNotFoundException("Organization not found: " + request.getOrgSubdomain());
                });

        if (!org.getOrgId().equals(user.getOrgId())) {
            logger.warn("Skipping forgotPassword for User: {} - User does not belong to organization: {}",
                    request.getEmail(), request.getOrgSubdomain());
            throw new ResourceNotFoundException("User not found in organization: " + request.getOrgSubdomain());
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            logger.warn("Skipping forgotPassword for User: {} - Account inactive", request.getEmail());
            throw new AccountInactiveException("User is inactive");
        }

        if (!Boolean.TRUE.equals(org.getIsActive())) {
            logger.warn("Skipping forgotPassword for User: {} - Organization inactive: {}",
                    request.getEmail(), request.getOrgSubdomain());
            throw new OrganizationInactiveException("Organization is inactive: " + request.getOrgSubdomain());
        }

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        String hashedTempPassword = passwordEncoder.encode(tempPassword);

        String redisKey = PR_PREFIX + org.getOrgId() + ":" + user.getEmail().toLowerCase();
        redisTemplate.opsForValue().set(redisKey, hashedTempPassword, Duration.ofMinutes(5));

        eventPublisher.publishEvent(new EmailDispatchEvent(
                user.getEmail(),
                "Password Reset Request",
                "reset-password",
                org.getOrgId(),
                java.util.Map.of(
                        "name", user.getEmail(),
                        "tempPassword", tempPassword)));

        logger.info("forgotPassword completed successfully for User: {}", request.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        logger.info("Processing resetPassword for User: {}", request.getEmail());

        // 1. Find User by email and validate organization
        AppUser user = appUserRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> {
                    logger.warn("Skipping resetPassword for User: {} - User not found", request.getEmail());
                    return new ResourceNotFoundException("User not found: " + request.getEmail());
                });

        Organization org = organizationRepository.findBySubdomainIgnoreCase(request.getOrgSubdomain())
                .orElseThrow(() -> {
                    logger.warn("Skipping resetPassword for User: {} - Organization not found: {}",
                            request.getEmail(), request.getOrgSubdomain());
                    return new ResourceNotFoundException("Organization not found: " + request.getOrgSubdomain());
                });

        if (!org.getOrgId().equals(user.getOrgId())) {
            logger.warn("Skipping resetPassword for User: {} - User does not belong to organization: {}",
                    request.getEmail(), request.getOrgSubdomain());
            throw new ResourceNotFoundException("User not found in organization: " + request.getOrgSubdomain());
        }

        String redisKey = PR_PREFIX + org.getOrgId() + ":" + user.getEmail().toLowerCase();
        String storedHash = redisTemplate.opsForValue().get(redisKey);

        if (storedHash == null || !passwordEncoder.matches(request.getTemporaryPassword(), storedHash)) {
            logger.warn("Skipping resetPassword for User: {} - Invalid or expired temporary password",
                    request.getEmail());
            throw new InvalidTokenException("Invalid or expired temporary password");
        }

        // 2. Set Tenant Context to ensure Auditing captures correct org_id
        TenantContextHolder.setTenantId(org.getOrgId());
        try {
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            user.setIsPasswordChanged(true);
            appUserRepository.save(user);
            redisTemplate.delete(redisKey);
        } finally {
            TenantContextHolder.clear();
        }

        logger.info("resetPassword completed successfully for User: {}", request.getEmail());
    }

    private String createRefreshToken(UUID userId) {
        String tokenValue = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                RT_PREFIX + tokenValue,
                userId.toString(),
                Duration.ofMillis(refreshExpirationMs));
        return tokenValue;
    }

    private void blacklistAccessToken(String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            tokenBlacklistService.blacklistToken(accessToken);
        }
    }

    private UUID resolveTokenOrgId(AppUser user) {
        if (ROLE_SUPER_ADMIN.equals(user.getRole().getRoleName())) {
            return null;
        }
        return user.getOrgId();
    }
}
