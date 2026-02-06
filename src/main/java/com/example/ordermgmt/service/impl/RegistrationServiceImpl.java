package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import com.example.ordermgmt.service.RegistrationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

// @Service: Marks this class as a Service Component where business logic lives.
@Service
public class RegistrationServiceImpl implements RegistrationService {

    // Dependencies we need to do the job
    private final AppUserRepository appUserRepository; // To save the new user
    private final UserRoleRepository userRoleRepository; // To look up the role
    private final PasswordEncoder passwordEncoder; // To encrypt the password

    // Constructor Injection:
    // Spring Boot automatically gives us the repositories and encoder we need.
    // We don't use 'new AppUserRepository()' ourselves.
    public RegistrationServiceImpl(AppUserRepository appUserRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String registerUser(RegistrationRequestDTO request) {
        System.out.println("  >>> [RegistrationServiceImpl] Processing registration logic...");

        // Step 1: Validation - Check if email is already taken
        if (appUserRepository.findByEmail(request.getEmail()).isPresent()) {
            System.out.println("  >>> [RegistrationServiceImpl] Validation Failed: Email already exists.");
            return "Email already exists";
        }

        // Step 2: Validation - Check if the requested Role exists (e.g., "CUSTOMER")
        UserRole role = userRoleRepository.findByRoleName(request.getRoleName())
                .orElse(null);
        if (role == null) {
            System.out.println("  >>> [RegistrationServiceImpl] Validation Failed: Role '" + request.getRoleName()
                    + "' not found.");
            return "Role not found";
        }

        // Step 3: Create the User Entity
        System.out.println("  >>> [RegistrationServiceImpl] Creating new AppUser entity...");
        AppUser newUser = new AppUser();
        // Generate a random unique ID for the user
        newUser.setUserId(UUID.randomUUID().toString());
        newUser.setEmail(request.getEmail());

        // ENCRYPT the password! Never save plain text passwords.
        // If DB is hacked, users are safe.
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        newUser.setRole(role);
        newUser.setIsActive(true); // User is active immediately
        newUser.setCreatedTimestamp(LocalDateTime.now());

        // Step 4: Save to Database using the Repository
        appUserRepository.save(newUser);
        System.out.println("  >>> [RegistrationServiceImpl] User saved successfully to database.");

        return "Registration successful";
    }
}
