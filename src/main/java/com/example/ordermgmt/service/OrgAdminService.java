package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.CreateAdminRequestDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.dto.UserResponseDTO;
import java.util.List;
import java.util.UUID;

public interface OrgAdminService {
    UserResponseDTO createAdmin(String requesterEmail, CreateAdminRequestDTO request);

    List<UserResponseDTO> listAdmins(String requesterEmail);

    void updateAdminStatus(String requesterEmail, UUID adminUserId, UpdateUserStatusRequestDTO request);
}
