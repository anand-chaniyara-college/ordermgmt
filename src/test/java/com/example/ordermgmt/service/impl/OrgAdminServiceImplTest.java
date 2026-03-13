package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.CreateAdminRequestDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.dto.UserResponseDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.event.EmailDispatchEvent;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.ResourceNotFoundException;
import com.example.ordermgmt.exception.UserAlreadyExistsException;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrgAdminServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrgAdminServiceImpl orgAdminService;

    private UUID orgId;
    private UUID requesterId;
    private UUID adminUserId;
    private AppUser requester;
    private AppUser adminUser;
    private UserRole orgAdminRole;
    private UserRole adminRole;
    private CreateAdminRequestDTO createAdminRequest;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        requesterId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();

        orgAdminRole = new UserRole();
        orgAdminRole.setRoleName("ORG_ADMIN");

        adminRole = new UserRole();
        adminRole.setRoleName("ADMIN");

        requester = new AppUser();
        requester.setUserId(requesterId);
        requester.setEmail("orgadmin@example.com");
        requester.setRole(orgAdminRole);
        requester.setOrgId(orgId);

        adminUser = new AppUser();
        adminUser.setUserId(adminUserId);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(adminRole);
        adminUser.setOrgId(orgId);
        adminUser.setIsActive(true);
        adminUser.setCreatedTimestamp(LocalDateTime.now());

        createAdminRequest = new CreateAdminRequestDTO();
        createAdminRequest.setEmail("newadmin@example.com");
        createAdminRequest.setPassword("password123");
    }

    @Test
    void createAdmin_WithValidRequest_CreatesSuccessfully() {
        when(appUserRepository.findByEmail("orgadmin@example.com")).thenReturn(Optional.of(requester));
        when(appUserRepository.existsByOrgIdAndEmailIgnoreCase(orgId, "newadmin@example.com")).thenReturn(false);
        when(userRoleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        AppUser savedAdmin = new AppUser();
        savedAdmin.setUserId(adminUserId);
        savedAdmin.setEmail("newadmin@example.com");
        savedAdmin.setRole(adminRole);
        savedAdmin.setOrgId(orgId);
        savedAdmin.setIsActive(true);
        savedAdmin.setCreatedTimestamp(LocalDateTime.now());
        
        when(appUserRepository.save(any(AppUser.class))).thenReturn(savedAdmin);

        UserResponseDTO response = orgAdminService.createAdmin("orgadmin@example.com", createAdminRequest);

        assertNotNull(response);
        assertEquals(adminUserId, response.getUserId());
        assertEquals("newadmin@example.com", response.getEmail());
        assertEquals("ADMIN", response.getRole());
        assertEquals(orgId, response.getOrgId());
        assertTrue(response.getIsActive());
        assertEquals("Admin created successfully.", response.getMessage());

        verify(eventPublisher).publishEvent(any(EmailDispatchEvent.class));
    }

    @Test
    void createAdmin_WithNonExistingRequester_ThrowsException() {
        when(appUserRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                orgAdminService.createAdmin("unknown@example.com", createAdminRequest));
    }

    @Test
    void createAdmin_WithNonOrgAdminRequester_ThrowsException() {
        AppUser nonOrgAdmin = new AppUser();
        nonOrgAdmin.setRole(new UserRole());
        nonOrgAdmin.getRole().setRoleName("CUSTOMER");
        
        when(appUserRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(nonOrgAdmin));

        assertThrows(InvalidOperationException.class, () -> 
                orgAdminService.createAdmin("customer@example.com", createAdminRequest));
    }

    @Test
    void createAdmin_WithExistingEmail_ThrowsException() {
        when(appUserRepository.findByEmail("orgadmin@example.com")).thenReturn(Optional.of(requester));
        when(appUserRepository.existsByOrgIdAndEmailIgnoreCase(orgId, "newadmin@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> 
                orgAdminService.createAdmin("orgadmin@example.com", createAdminRequest));
    }

    @Test
    void listAdmins_ReturnsAllAdminsInOrg() {
        when(appUserRepository.findByEmail("orgadmin@example.com")).thenReturn(Optional.of(requester));
        when(appUserRepository.findByRole_RoleNameAndOrgId("ADMIN", orgId)).thenReturn(List.of(adminUser));

        List<UserResponseDTO> result = orgAdminService.listAdmins("orgadmin@example.com");

        assertEquals(1, result.size());
        assertEquals(adminUserId, result.get(0).getUserId());
        assertEquals("admin@example.com", result.get(0).getEmail());
        assertEquals("ADMIN", result.get(0).getRole());
    }

    @Test
    void listAdmins_WithNoAdmins_ReturnsEmptyList() {
        when(appUserRepository.findByEmail("orgadmin@example.com")).thenReturn(Optional.of(requester));
        when(appUserRepository.findByRole_RoleNameAndOrgId("ADMIN", orgId)).thenReturn(List.of());

        List<UserResponseDTO> result = orgAdminService.listAdmins("orgadmin@example.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void updateAdminStatus_WithValidRequest_UpdatesSuccessfully() {
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO(false);
        
        when(appUserRepository.findByEmail("orgadmin@example.com")).thenReturn(Optional.of(requester));
        when(appUserRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));

        orgAdminService.updateAdminStatus("orgadmin@example.com", adminUserId, request);

        assertFalse(adminUser.getIsActive());
        verify(appUserRepository).save(adminUser);
    }

    @Test
    void updateAdminStatus_WithNonExistingAdmin_ThrowsException() {
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO(false);
        
        when(appUserRepository.findByEmail("orgadmin@example.com")).thenReturn(Optional.of(requester));
        when(appUserRepository.findById(adminUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                orgAdminService.updateAdminStatus("orgadmin@example.com", adminUserId, request));
    }

    @Test
    void updateAdminStatus_WithNonAdminUser_ThrowsException() {
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO(false);
        
        AppUser nonAdmin = new AppUser();
        nonAdmin.setUserId(UUID.randomUUID());
        nonAdmin.setRole(new UserRole());
        nonAdmin.getRole().setRoleName("CUSTOMER");
        nonAdmin.setOrgId(orgId);

        when(appUserRepository.findByEmail("orgadmin@example.com")).thenReturn(Optional.of(requester));
        when(appUserRepository.findById(nonAdmin.getUserId())).thenReturn(Optional.of(nonAdmin));

        assertThrows(InvalidOperationException.class, () -> 
                orgAdminService.updateAdminStatus("orgadmin@example.com", nonAdmin.getUserId(), request));
    }

    @Test
    void updateAdminStatus_WithCrossOrgAdmin_ThrowsException() {
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO(false);
        
        AppUser otherOrgAdmin = new AppUser();
        otherOrgAdmin.setUserId(adminUserId);
        otherOrgAdmin.setRole(adminRole);
        otherOrgAdmin.setOrgId(UUID.randomUUID()); // Different org

        when(appUserRepository.findByEmail("orgadmin@example.com")).thenReturn(Optional.of(requester));
        when(appUserRepository.findById(adminUserId)).thenReturn(Optional.of(otherOrgAdmin));

        assertThrows(InvalidOperationException.class, () -> 
                orgAdminService.updateAdminStatus("orgadmin@example.com", adminUserId, request));
    }
}
