package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.CreateOrgAdminRequestDTO;
import com.example.ordermgmt.dto.CreateOrganizationRequestDTO;
import com.example.ordermgmt.dto.OrganizationResponseDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.dto.UserResponseDTO;
import java.util.List;
import java.util.UUID;

public interface SuperAdminService {
    OrganizationResponseDTO createOrganization(CreateOrganizationRequestDTO request);

    List<OrganizationResponseDTO> listOrganizations();

    UserResponseDTO createOrgAdmin(CreateOrgAdminRequestDTO request);

    List<UserResponseDTO> listOrgAdmins();

    void updateOrgAdminStatus(UUID userId, UpdateUserStatusRequestDTO request);
}
