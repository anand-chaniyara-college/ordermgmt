package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.CreateOrgAdminRequestDTO;
import com.example.ordermgmt.dto.CreateOrganizationRequestDTO;
import com.example.ordermgmt.dto.OrganizationResponseDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.dto.UserResponseDTO;
import com.example.ordermgmt.exception.GlobalExceptionHandler;
import com.example.ordermgmt.service.SuperAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SuperAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SuperAdminService superAdminService;

    @InjectMocks
    private SuperAdminController superAdminController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(superAdminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateOrganization_Success() throws Exception {
        CreateOrganizationRequestDTO requestDTO = new CreateOrganizationRequestDTO(
                "Acme Corporation",
                "acme",
                "Demo organization");

        OrganizationResponseDTO responseDTO = new OrganizationResponseDTO();
        responseDTO.setOrgId(UUID.randomUUID());
        responseDTO.setName("Acme Corporation");
        responseDTO.setSubdomain("acme");
        responseDTO.setIsActive(true);

        when(superAdminService.createOrganization(any(CreateOrganizationRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/super-admin/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme Corporation"))
                .andExpect(jsonPath("$.subdomain").value("acme"));

        verify(superAdminService, times(1)).createOrganization(any(CreateOrganizationRequestDTO.class));
    }

    @Test
    void testCreateOrganization_BadRequest_ValidationFailure() throws Exception {
        CreateOrganizationRequestDTO requestDTO = new CreateOrganizationRequestDTO("", "bad subdomain!", "desc");

        mockMvc.perform(post("/api/super-admin/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());

        verify(superAdminService, never()).createOrganization(any(CreateOrganizationRequestDTO.class));
    }

    @Test
    void testListOrganizations_Success() throws Exception {
        OrganizationResponseDTO org = new OrganizationResponseDTO();
        org.setOrgId(UUID.randomUUID());
        org.setName("Acme Corporation");
        org.setSubdomain("acme");

        when(superAdminService.listOrganizations()).thenReturn(Collections.singletonList(org));

        mockMvc.perform(get("/api/super-admin/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizations").isArray())
                .andExpect(jsonPath("$.organizations[0].name").value("Acme Corporation"));

        verify(superAdminService, times(1)).listOrganizations();
    }

    @Test
    void testCreateOrgAdmin_Success() throws Exception {
        CreateOrgAdminRequestDTO requestDTO = new CreateOrgAdminRequestDTO(
                "org-admin@example.com",
                "password123",
                UUID.randomUUID());

        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setUserId(UUID.randomUUID());
        responseDTO.setEmail("org-admin@example.com");
        responseDTO.setRole("ORG_ADMIN");
        responseDTO.setIsActive(true);

        when(superAdminService.createOrgAdmin(any(CreateOrgAdminRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/super-admin/org-admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("org-admin@example.com"))
                .andExpect(jsonPath("$.role").value("ORG_ADMIN"));

        verify(superAdminService, times(1)).createOrgAdmin(any(CreateOrgAdminRequestDTO.class));
    }

    @Test
    void testListOrgAdmins_Success() throws Exception {
        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(UUID.randomUUID());
        user.setEmail("org-admin@example.com");
        user.setRole("ORG_ADMIN");

        when(superAdminService.listOrgAdmins()).thenReturn(Collections.singletonList(user));

        mockMvc.perform(get("/api/super-admin/org-admins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgAdmins").isArray())
                .andExpect(jsonPath("$.orgAdmins[0].email").value("org-admin@example.com"));

        verify(superAdminService, times(1)).listOrgAdmins();
    }

    @Test
    void testUpdateOrgAdminStatus_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserStatusRequestDTO requestDTO = new UpdateUserStatusRequestDTO(false);
        doNothing().when(superAdminService).updateOrgAdminStatus(eq(userId), any(UpdateUserStatusRequestDTO.class));

        mockMvc.perform(patch("/api/super-admin/org-admins/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Org Admin status updated successfully."));

        verify(superAdminService, times(1)).updateOrgAdminStatus(eq(userId), any(UpdateUserStatusRequestDTO.class));
    }

    @Test
    void testUpdateOrgAdminStatus_BadRequest_InvalidUserId() throws Exception {
        UpdateUserStatusRequestDTO requestDTO = new UpdateUserStatusRequestDTO(true);

        mockMvc.perform(patch("/api/super-admin/org-admins/{id}/status", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());

        verify(superAdminService, never()).updateOrgAdminStatus(any(), any(UpdateUserStatusRequestDTO.class));
    }

    @Test
    void testCreateOrgAdmin_InternalServerError() throws Exception {
        CreateOrgAdminRequestDTO requestDTO = new CreateOrgAdminRequestDTO(
                "org-admin@example.com",
                "password123",
                UUID.randomUUID());

        doThrow(new RuntimeException("Service failure"))
                .when(superAdminService).createOrgAdmin(any(CreateOrgAdminRequestDTO.class));

        mockMvc.perform(post("/api/super-admin/org-admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isInternalServerError());
    }
}
