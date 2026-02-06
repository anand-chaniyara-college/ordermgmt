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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final com.example.ordermgmt.repository.RefreshTokenRepository refreshTokenRepository; // New Repo
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(AppUserRepository appUserRepository,
            UserRoleRepository userRoleRepository,
            com.example.ordermgmt.repository.RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String registerUser(RegistrationRequestDTO request) {
        System.out.println("  >>> [AuthServiceImpl] Processing subscription logic...");

        // 1. Check Email
        if (appUserRepository.findByEmail(request.getEmail()).isPresent()) {
            return "Email already exists";
        }

        // 2. Check Role
        UserRole role = userRoleRepository.findByRoleName(request.getRoleName()).orElse(null);
        if (role == null) {
            return "Role not found";
        }

        // 3. Create User
        AppUser newUser = new AppUser();
        newUser.setUserId(UUID.randomUUID().toString());
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(role);
        newUser.setIsActive(true);
        newUser.setCreatedTimestamp(LocalDateTime.now());

        // 4. Save
        appUserRepository.save(newUser);
        return "Registration successful";
    }

    @Override
    public LoginResponseDTO loginUser(LoginRequestDTO request) {
        System.out.println("  >>> [AuthServiceImpl] Processing login logic for: " + request.getEmail());

        // Step 1: Check if User exists in Database
        // We use .orElse(null) to handle the case where email is not found gracefully
        AppUser user = appUserRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return new LoginResponseDTO(null, null, null, "User not found");
        }

        // Step 2: Validate Password
        // We MUST use passwordEncoder.matches() because the DB has the HASHED version.
        // We cannot just compare strings (request.getPassword().equals(dbHash) would
        // FAIL)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return new LoginResponseDTO(null, null, null, "Invalid credentials");
        }

        // Step 3: Check if Active
        // Security check to ensure banned/inactive users cannot log in
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            return new LoginResponseDTO(null, null, null, "User is inactive");
        }

        // Step 4: Generate JWT Token (Access Token)
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleName());

        // Step 5: Generate & Save Refresh Token
        RefreshToken refreshToken = createRefreshToken(user);

        System.out.println("  >>> [AuthServiceImpl] Login successful. Associated Tokens generated.");

        // Return the tokens + role to the controller
        return new LoginResponseDTO(accessToken, refreshToken.getToken(), user.getRole().getRoleName(),
                "Login successful");
    }

    @Override
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        System.out.println("  >>> [AuthServiceImpl] Processing Refresh Token request...");

        String requestToken = request.getRefreshToken();

        // 1. Find Token in DB
        return refreshTokenRepository.findByToken(requestToken)
                .map(token -> {
                    // 2. Check Expiry
                    if (token.getExpiryDate().isBefore(java.time.Instant.now())) {
                        System.out.println("    >>> Token Expired!");
                        // Cannot refresh, return error (in real app, throw exception)
                        return new RefreshTokenResponseDTO(null, null, "Bearer",
                                "Refresh token was expired. Please make a new signin request");
                    }

                    // 3. Generate NEW Access Token
                    AppUser user = token.getAppUser();
                    String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleName());

                    System.out.println("    >>> New Access Token generated.");
                    return new RefreshTokenResponseDTO(newAccessToken, requestToken, "Bearer",
                            "Token refreshed successfully");
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    // Helper method to create and save a Refresh Token
    private RefreshToken createRefreshToken(AppUser user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenId(UUID.randomUUID().toString());
        refreshToken.setAppUser(user);
        refreshToken.setExpiryDate(java.time.Instant.now().plusMillis(1000L * 60 * 60 * 24 * 30)); // 30 Days
        refreshToken.setToken(UUID.randomUUID().toString()); // Secure Random String
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public String logoutUser(RefreshTokenRequestDTO request) {
        System.out.println("  >>> [AuthServiceImpl] Processing Logout request...");

        String requestToken = request.getRefreshToken();

        // Find and "Revoke" the token from DB
        refreshTokenRepository.findByToken(requestToken).ifPresent(token -> {
            // We don't delete it, we just mark it as revoked (Safety/Audit reasons)
            // Or you COULD delete it: refreshTokenRepository.delete(token);
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            System.out.println("    >>> Refresh Token Revoked.");
        });

        return "Logout successful";
    }
}
