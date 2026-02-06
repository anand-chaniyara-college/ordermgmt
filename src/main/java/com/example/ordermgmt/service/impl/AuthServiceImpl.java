package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.entity.AppUser;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(AppUserRepository appUserRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
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
            return new LoginResponseDTO(null, null, "User not found");
        }

        // Step 2: Validate Password
        // We MUST use passwordEncoder.matches() because the DB has the HASHED version.
        // We cannot just compare strings (request.getPassword().equals(dbHash) would
        // FAIL)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return new LoginResponseDTO(null, null, "Invalid credentials");
        }

        // Step 3: Check if Active
        // Security check to ensure banned/inactive users cannot log in
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            return new LoginResponseDTO(null, null, "User is inactive");
        }

        // Step 4: Generate JWT Token
        // If we reached here, the user is valid!
        // We generate a digital "Access Pass" (Token) for them.
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleName());

        System.out.println("  >>> [AuthServiceImpl] Login successful. Token generated.");

        // Return the token + role to the controller
        return new LoginResponseDTO(token, user.getRole().getRoleName(), "Login successful");
    }
}
