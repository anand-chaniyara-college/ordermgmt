package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.CreateOrgAdminRequestDTO;
import com.example.ordermgmt.dto.CreateOrganizationRequestDTO;
import com.example.ordermgmt.dto.UpdateOrganizationStatusRequestDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.dto.OrganizationResponseDTO;
import com.example.ordermgmt.dto.UserResponseDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Organization;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.event.EmailDispatchEvent;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.ResourceNotFoundException;
import com.example.ordermgmt.exception.UserAlreadyExistsException;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.OrganizationRepository;
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
class SuperAdminServiceImplTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SuperAdminServiceImpl superAdminService;

    private UUID orgId;
    private UUID userId;
    private Organization organization;
    private AppUser orgAdmin;
    private UserRole orgAdminRole;
    private CreateOrganizationRequestDTO createOrgRequest;
    private CreateOrgAdminRequestDTO createOrgAdminRequest;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();

        organization = new Organization();
        organization.setOrgId(orgId);
        organization.setName("Test Org");
        organization.setSubdomain("testorg");
        organization.setDescription("Test Description");
        organization.setIsActive(true);
        organization.setCreatedTimestamp(LocalDateTime.now());

        orgAdminRole = new UserRole();
        orgAdminRole.setRoleName("ORG_ADMIN");

        orgAdmin = new AppUser();
        orgAdmin.setUserId(userId);
        orgAdmin.setEmail("orgadmin@example.com");
        orgAdmin.setRole(orgAdminRole);
        orgAdmin.setOrgId(orgId);
        orgAdmin.setIsActive(true);
        orgAdmin.setCreatedTimestamp(LocalDateTime.now());

        createOrgRequest = new CreateOrganizationRequestDTO();
        createOrgRequest.setName("New Org");
        createOrgRequest.setSubdomain("neworg");
        createOrgRequest.setDescription("New Description");

        createOrgAdminRequest = new CreateOrgAdminRequestDTO();
        createOrgAdminRequest.setOrgId(orgId);
        createOrgAdminRequest.setEmail("newadmin@example.com");
        createOrgAdminRequest.setPassword("password123");
    }

    @Test
    void createOrganization_WithValidRequest_CreatesSuccessfully() {
        when(organizationRepository.findBySubdomainIgnoreCase("neworg")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization saved = invocation.getArgument(0);
            saved.setOrgId(orgId);
            return saved;
        });

        OrganizationResponseDTO response = superAdminService.createOrganization(createOrgRequest);

        assertNotNull(response);
        assertEquals(orgId, response.getOrgId());
        assertEquals("New Org", response.getName());
        assertEquals("neworg", response.getSubdomain());
        assertEquals("New Description", response.getDescription());
        assertTrue(response.getIsActive());
        assertNotNull(response.getCreatedTimestamp());
    }

    @Test
    void createOrganization_WithExistingSubdomain_ThrowsException() {
        when(organizationRepository.findBySubdomainIgnoreCase("neworg")).thenReturn(Optional.of(organization));

        assertThrows(UserAlreadyExistsException.class, () -> 
                superAdminService.createOrganization(createOrgRequest));
    }

    @Test
    void listOrganizations_ReturnsAllOrganizations() {
        when(organizationRepository.findAll()).thenReturn(List.of(organization));

        List<OrganizationResponseDTO> result = superAdminService.listOrganizations();

        assertEquals(1, result.size());
        assertEquals(orgId, result.get(0).getOrgId());
        assertEquals("Test Org", result.get(0).getName());
        assertEquals("testorg", result.get(0).getSubdomain());
    }

    @Test
    void listOrganizations_WithNoOrganizations_ReturnsEmptyList() {
        when(organizationRepository.findAll()).thenReturn(List.of());

        List<OrganizationResponseDTO> result = superAdminService.listOrganizations();

        assertTrue(result.isEmpty());
    }

    @Test
    void createOrgAdmin_WithValidRequest_CreatesSuccessfully() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(appUserRepository.existsByOrgIdAndEmailIgnoreCase(orgId, "newadmin@example.com")).thenReturn(false);
        when(userRoleRepository.findByRoleName("ORG_ADMIN")).thenReturn(Optional.of(orgAdminRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        AppUser savedAdmin = new AppUser();
        savedAdmin.setUserId(userId);
        savedAdmin.setEmail("newadmin@example.com");
        savedAdmin.setRole(orgAdminRole);
        savedAdmin.setOrgId(orgId);
        savedAdmin.setIsActive(true);
        savedAdmin.setCreatedTimestamp(LocalDateTime.now());
        
        when(appUserRepository.save(any(AppUser.class))).thenReturn(savedAdmin);

        UserResponseDTO response = superAdminService.createOrgAdmin(createOrgAdminRequest);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("newadmin@example.com", response.getEmail());
        assertEquals("ORG_ADMIN", response.getRole());
        assertEquals(orgId, response.getOrgId());
        assertTrue(response.getIsActive());
        assertEquals("Org Admin created successfully.", response.getMessage());

        verify(eventPublisher).publishEvent(any(EmailDispatchEvent.class));
    }

    @Test
    void createOrgAdmin_WithNullOrgId_ThrowsException() {
        createOrgAdminRequest.setOrgId(null);

        assertThrows(InvalidOperationException.class, () -> 
                superAdminService.createOrgAdmin(createOrgAdminRequest));
    }

    @Test
    void createOrgAdmin_WithNonExistingOrganization_ThrowsException() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                superAdminService.createOrgAdmin(createOrgAdminRequest));
    }

    @Test
    void createOrgAdmin_WithExistingEmail_ThrowsException() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(appUserRepository.existsByOrgIdAndEmailIgnoreCase(orgId, "newadmin@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> 
                superAdminService.createOrgAdmin(createOrgAdminRequest));
    }

    @Test
    void listOrgAdmins_ReturnsAllOrgAdmins() {
        when(appUserRepository.findByRole_RoleName("ORG_ADMIN")).thenReturn(List.of(orgAdmin));

        List<UserResponseDTO> result = superAdminService.listOrgAdmins();

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals("orgadmin@example.com", result.get(0).getEmail());
        assertEquals("ORG_ADMIN", result.get(0).getRole());
    }

    @Test
    void listOrgAdmins_WithNoAdmins_ReturnsEmptyList() {
        when(appUserRepository.findByRole_RoleName("ORG_ADMIN")).thenReturn(List.of());

        List<UserResponseDTO> result = superAdminService.listOrgAdmins();

        assertTrue(result.isEmpty());
    }

    @Test
    void updateOrgAdminStatus_WithValidRequest_UpdatesSuccessfully() {
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO(false);
        
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(orgAdmin));

        superAdminService.updateOrgAdminStatus(userId, request);

        assertFalse(orgAdmin.getIsActive());
        verify(appUserRepository).save(orgAdmin);
    }

    @Test
    void updateOrgAdminStatus_WithNonExistingUser_ThrowsException() {
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO(false);
        
        when(appUserRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                superAdminService.updateOrgAdminStatus(userId, request));
    }

    @Test
    void updateOrgAdminStatus_WithNonOrgAdminUser_ThrowsException() {
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO(false);
        
        AppUser nonOrgAdmin = new AppUser();
        nonOrgAdmin.setUserId(UUID.randomUUID());
        nonOrgAdmin.setRole(new UserRole());
        nonOrgAdmin.getRole().setRoleName("CUSTOMER");

        when(appUserRepository.findById(nonOrgAdmin.getUserId())).thenReturn(Optional.of(nonOrgAdmin));

        assertThrows(InvalidOperationException.class, () -> 
                superAdminService.updateOrgAdminStatus(nonOrgAdmin.getUserId(), request));
    }

    @Test
    void updateOrganizationStatus_WithValidRequest_UpdatesSuccessfully() {
        UpdateOrganizationStatusRequestDTO request = new UpdateOrganizationStatusRequestDTO(false);
        
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        superAdminService.updateOrganizationStatus(orgId, request);

        assertFalse(organization.getIsActive());
        verify(organizationRepository).save(organization);
    }

    @Test
    void updateOrganizationStatus_WithNonExistingOrg_ThrowsException() {
        UpdateOrganizationStatusRequestDTO request = new UpdateOrganizationStatusRequestDTO(false);
        
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                superAdminService.updateOrganizationStatus(orgId, request));
    }

    @Test
    void createOrgAdmin_WithInactiveOrganization_StillCreatesAdmin() {
        organization.setIsActive(false);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(appUserRepository.existsByOrgIdAndEmailIgnoreCase(orgId, "newadmin@example.com")).thenReturn(false);
        when(userRoleRepository.findByRoleName("ORG_ADMIN")).thenReturn(Optional.of(orgAdminRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        AppUser savedAdmin = new AppUser();
        savedAdmin.setUserId(userId);
        savedAdmin.setEmail("newadmin@example.com");
        savedAdmin.setRole(orgAdminRole);
        savedAdmin.setOrgId(orgId);
        savedAdmin.setIsActive(true);
        
        when(appUserRepository.save(any(AppUser.class))).thenReturn(savedAdmin);

        UserResponseDTO response = superAdminService.createOrgAdmin(createOrgAdminRequest);

        assertNotNull(response);
        verify(eventPublisher).publishEvent(any(EmailDispatchEvent.class));
    }
}
