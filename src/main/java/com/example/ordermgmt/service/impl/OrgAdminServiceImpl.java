package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.CreateAdminRequestDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.dto.UserResponseDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.ResourceNotFoundException;
import com.example.ordermgmt.exception.UserAlreadyExistsException;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import com.example.ordermgmt.service.OrgAdminService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgAdminServiceImpl implements OrgAdminService {

    private static final Logger logger = LoggerFactory.getLogger(OrgAdminServiceImpl.class);
    private static final String ROLE_ORG_ADMIN = "ORG_ADMIN";
    private static final String ROLE_ADMIN = "ADMIN";

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public OrgAdminServiceImpl(
            AppUserRepository appUserRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponseDTO createAdmin(String requesterEmail, CreateAdminRequestDTO request) {
        logger.info("Processing createAdmin for User: {}", request.getEmail());

        AppUser requester = getOrgAdminRequester(requesterEmail);

        if (appUserRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Skipping createAdmin for User: {} - Email already exists", request.getEmail());
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        UserRole adminRole = userRoleRepository.findByRoleName(ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + ROLE_ADMIN));

        AppUser admin = new AppUser();
        admin.setEmail(request.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setRole(adminRole);
        admin.setOrgId(requester.getOrgId());
        admin.setIsActive(true);

        AppUser saved = appUserRepository.save(admin);
        logger.info("createAdmin completed successfully for User: {}", request.getEmail());

        return new UserResponseDTO(
                saved.getUserId(),
                saved.getEmail(),
                saved.getRole().getRoleName(),
                saved.getOrgId(),
                saved.getIsActive(),
                saved.getCreatedTimestamp(),
                "Admin created successfully.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> listAdmins(String requesterEmail) {
        logger.info("Processing listAdmins for Entity: APP_USER");
        AppUser requester = getOrgAdminRequester(requesterEmail);

        List<UserResponseDTO> admins = appUserRepository.findByRole_RoleNameAndOrgId(ROLE_ADMIN, requester.getOrgId())
                .stream()
                .map(user -> new UserResponseDTO(
                        user.getUserId(),
                        user.getEmail(),
                        user.getRole().getRoleName(),
                        user.getOrgId(),
                        user.getIsActive(),
                        user.getCreatedTimestamp(),
                        null))
                .toList();

        logger.info("listAdmins completed successfully for Entity: APP_USER");
        return admins;
    }

    @Override
    @Transactional
    public void updateAdminStatus(String requesterEmail, UUID adminUserId, UpdateUserStatusRequestDTO request) {
        logger.info("Processing updateAdminStatus for User: {}", adminUserId);
        AppUser requester = getOrgAdminRequester(requesterEmail);

        AppUser admin = appUserRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + adminUserId));

        if (!ROLE_ADMIN.equals(admin.getRole().getRoleName())) {
            logger.warn("Skipping updateAdminStatus for User: {} - User is not ADMIN", adminUserId);
            throw new InvalidOperationException("Target user is not ADMIN");
        }

        if (!requester.getOrgId().equals(admin.getOrgId())) {
            logger.warn("Skipping updateAdminStatus for User: {} - Cross-org update is not allowed", adminUserId);
            throw new InvalidOperationException("Cross-organization admin update is not allowed");
        }

        admin.setIsActive(request.getIsActive());
        appUserRepository.save(admin);
        logger.info("updateAdminStatus completed successfully for User: {}", adminUserId);
    }

    private AppUser getOrgAdminRequester(String requesterEmail) {
        AppUser requester = appUserRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Requester not found: " + requesterEmail));

        if (!ROLE_ORG_ADMIN.equals(requester.getRole().getRoleName())) {
            throw new InvalidOperationException("Requester is not ORG_ADMIN");
        }
        return requester;
    }
}
