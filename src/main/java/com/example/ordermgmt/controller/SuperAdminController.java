package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.CreateOrgAdminRequestDTO;
import com.example.ordermgmt.dto.CreateOrganizationRequestDTO;
import com.example.ordermgmt.dto.OrganizationResponseDTO;
import com.example.ordermgmt.dto.UpdateOrganizationStatusRequestDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.dto.UserResponseDTO;
import com.example.ordermgmt.service.SuperAdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super-admin")
@Validated
public class SuperAdminController {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminController.class);

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @PostMapping("/organizations")
    public ResponseEntity<OrganizationResponseDTO> createOrganization(
            @Valid @RequestBody CreateOrganizationRequestDTO request) {
        logger.info("Processing createOrganization for SuperAdmin");
        OrganizationResponseDTO response = superAdminService.createOrganization(request);
        logger.info("createOrganization completed successfully for SuperAdmin");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/organizations")
    public ResponseEntity<Map<String, List<OrganizationResponseDTO>>> listOrganizations() {
        logger.info("Processing listOrganizations for SuperAdmin");
        List<OrganizationResponseDTO> organizations = superAdminService.listOrganizations();
        logger.info("listOrganizations completed successfully for SuperAdmin");
        return ResponseEntity.ok(Map.of("organizations", organizations));
    }

    @PostMapping("/org-admins")
    public ResponseEntity<UserResponseDTO> createOrgAdmin(@Valid @RequestBody CreateOrgAdminRequestDTO request) {
        logger.info("Processing createOrgAdmin for SuperAdmin");
        UserResponseDTO response = superAdminService.createOrgAdmin(request);
        logger.info("createOrgAdmin completed successfully for SuperAdmin");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/org-admins")
    public ResponseEntity<Map<String, List<UserResponseDTO>>> listOrgAdmins() {
        logger.info("Processing listOrgAdmins for SuperAdmin");
        List<UserResponseDTO> users = superAdminService.listOrgAdmins();
        logger.info("listOrgAdmins completed successfully for SuperAdmin");
        return ResponseEntity.ok(Map.of("orgAdmins", users));
    }

    @PatchMapping("/org-admins/{id}/status")
    public ResponseEntity<Map<String, String>> updateOrgAdminStatus(
            @PathVariable("id") UUID userId,
            @Valid @RequestBody UpdateUserStatusRequestDTO request) {
        logger.info("Processing updateOrgAdminStatus for User: {}", userId);
        superAdminService.updateOrgAdminStatus(userId, request);
        logger.info("updateOrgAdminStatus completed successfully for User: {}", userId);
        return ResponseEntity.ok(Map.of("message", "Org Admin status updated successfully."));
    }

    @PatchMapping("/organizations/{id}/status")
    public ResponseEntity<Map<String, String>> updateOrganizationStatus(
            @PathVariable("id") UUID orgId,
            @Valid @RequestBody UpdateOrganizationStatusRequestDTO request) {
        logger.info("Processing updateOrganizationStatus for Organization: {}", orgId);
        superAdminService.updateOrganizationStatus(orgId, request);
        logger.info("updateOrganizationStatus completed successfully for Organization: {}", orgId);
        return ResponseEntity.ok(Map.of("message", "Organization status updated successfully."));
    }
}
