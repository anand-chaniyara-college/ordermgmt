package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.CreateOrgAdminRequestDTO;
import com.example.ordermgmt.dto.CreateOrganizationRequestDTO;
import com.example.ordermgmt.dto.OrganizationResponseDTO;
import com.example.ordermgmt.dto.UpdateOrganizationStatusRequestDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.dto.UserResponseDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Organization;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.ResourceNotFoundException;
import com.example.ordermgmt.exception.UserAlreadyExistsException;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.OrganizationRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import com.example.ordermgmt.service.SuperAdminService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuperAdminServiceImpl implements SuperAdminService {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminServiceImpl.class);
    private static final String ROLE_ORG_ADMIN = "ORG_ADMIN";

    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminServiceImpl(
            OrganizationRepository organizationRepository,
            AppUserRepository appUserRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OrganizationResponseDTO createOrganization(CreateOrganizationRequestDTO request) {
        logger.info("Processing createOrganization for Subdomain: {}", request.getSubdomain());

        if (organizationRepository.findBySubdomainIgnoreCase(request.getSubdomain()).isPresent()) {
            logger.warn("Skipping createOrganization for Subdomain: {} - Already exists", request.getSubdomain());
            throw new UserAlreadyExistsException("Organization subdomain already exists: " + request.getSubdomain());
        }

        Organization org = new Organization();
        org.setOrgId(UUID.randomUUID());
        org.setName(request.getName());
        org.setSubdomain(request.getSubdomain());
        org.setDescription(request.getDescription());
        org.setIsActive(true);
        org.setCreatedTimestamp(LocalDateTime.now());

        Organization saved = organizationRepository.save(org);
        logger.info("createOrganization completed successfully for Subdomain: {}", request.getSubdomain());
        return toOrganizationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponseDTO> listOrganizations() {
        logger.info("Processing listOrganizations for Entity: ORGANIZATION");
        List<OrganizationResponseDTO> organizations = organizationRepository.findAll().stream()
                .map(this::toOrganizationResponse)
                .toList();
        logger.info("listOrganizations completed successfully for Entity: ORGANIZATION");
        return organizations;
    }

    @Override
    @Transactional
    public UserResponseDTO createOrgAdmin(CreateOrgAdminRequestDTO request) {
        logger.info("Processing createOrgAdmin for User: {}", request.getEmail());

        if (request.getOrgId() == null) {
            logger.warn("Skipping createOrgAdmin for User: {} - orgId is required", request.getEmail());
            throw new InvalidOperationException("orgId is required");
        }

        Organization org = organizationRepository.findById(request.getOrgId())
                .orElseThrow(() -> {
                    logger.warn("Skipping createOrgAdmin for User: {} - Organization not found", request.getEmail());
                    return new ResourceNotFoundException("Organization not found: " + request.getOrgId());
                });

        if (appUserRepository.existsByOrgIdAndEmailIgnoreCase(org.getOrgId(), request.getEmail())) {
            logger.warn("Skipping createOrgAdmin for User: {} - Email already exists in org: {}",
                    request.getEmail(), org.getOrgId());
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        UserRole orgAdminRole = userRoleRepository.findByRoleName(ROLE_ORG_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + ROLE_ORG_ADMIN));

        AppUser orgAdmin = new AppUser();
        orgAdmin.setEmail(request.getEmail());
        orgAdmin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        orgAdmin.setRole(orgAdminRole);
        orgAdmin.setOrgId(org.getOrgId());
        orgAdmin.setIsActive(true);

        AppUser saved = appUserRepository.save(orgAdmin);
        logger.info("createOrgAdmin completed successfully for User: {}", request.getEmail());

        return new UserResponseDTO(
                saved.getUserId(),
                saved.getEmail(),
                saved.getRole().getRoleName(),
                saved.getOrgId(),
                saved.getIsActive(),
                saved.getCreatedTimestamp(),
                "Org Admin created successfully.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> listOrgAdmins() {
        logger.info("Processing listOrgAdmins for Entity: APP_USER");
        List<UserResponseDTO> orgAdmins = appUserRepository.findByRole_RoleName(ROLE_ORG_ADMIN).stream()
                .map(user -> new UserResponseDTO(
                        user.getUserId(),
                        user.getEmail(),
                        user.getRole().getRoleName(),
                        user.getOrgId(),
                        user.getIsActive(),
                        user.getCreatedTimestamp(),
                        null))
                .toList();
        logger.info("listOrgAdmins completed successfully for Entity: APP_USER");
        return orgAdmins;
    }

    @Override
    @Transactional
    public void updateOrgAdminStatus(UUID userId, UpdateUserStatusRequestDTO request) {
        logger.info("Processing updateOrgAdminStatus for User: {}", userId);

        AppUser orgAdmin = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (!ROLE_ORG_ADMIN.equals(orgAdmin.getRole().getRoleName())) {
            logger.warn("Skipping updateOrgAdminStatus for User: {} - User is not ORG_ADMIN", userId);
            throw new InvalidOperationException("Target user is not ORG_ADMIN");
        }

        orgAdmin.setIsActive(request.getIsActive());
        appUserRepository.save(orgAdmin);
        logger.info("updateOrgAdminStatus completed successfully for User: {}", userId);
    }

    @Override
    @Transactional
    public void updateOrganizationStatus(UUID orgId, UpdateOrganizationStatusRequestDTO request) {
        logger.info("Processing updateOrganizationStatus for Organization: {}", orgId);

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> {
                    logger.warn("Skipping updateOrganizationStatus for Organization: {} - Organization not found",
                            orgId);
                    return new ResourceNotFoundException("Organization not found: " + orgId);
                });

        org.setIsActive(request.getIsActive());
        organizationRepository.save(org);
        logger.info("updateOrganizationStatus completed successfully for Organization: {}", orgId);
    }

    private OrganizationResponseDTO toOrganizationResponse(Organization org) {
        return new OrganizationResponseDTO(
                org.getOrgId(),
                org.getName(),
                org.getSubdomain(),
                org.getIsActive(),
                org.getDescription(),
                org.getCreatedTimestamp());
    }
}
