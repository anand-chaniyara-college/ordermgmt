package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.*;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Organization;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.event.EmailDispatchEvent;
import com.example.ordermgmt.exception.*;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.CustomerRepository;
import com.example.ordermgmt.repository.OrganizationRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import com.example.ordermgmt.security.JwtUtil;
import com.example.ordermgmt.security.TenantContextHolder;
import com.example.ordermgmt.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID orgId;
    private UUID userId;
    private Organization organization;
    private AppUser appUser;
    private UserRole customerRole;
    private UserRole superAdminRole;
    private RegistrationRequestDTO registrationRequest;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        organization = new Organization();
        organization.setOrgId(orgId);
        organization.setSubdomain("testorg");
        organization.setIsActive(true);

        customerRole = new UserRole();
        customerRole.setRoleName("CUSTOMER");

        superAdminRole = new UserRole();
        superAdminRole.setRoleName("SUPER_ADMIN");

        appUser = new AppUser();
        appUser.setUserId(userId);
        appUser.setEmail("test@example.com");
        appUser.setPasswordHash("encodedPassword");
        appUser.setRole(customerRole);
        appUser.setOrgId(orgId);
        appUser.setIsActive(true);

        registrationRequest = new RegistrationRequestDTO();
        registrationRequest.setEmail("new@example.com");
        registrationRequest.setPassword("password123");
        registrationRequest.setOrgSubdomain("testorg");
        registrationRequest.setRoleName("CUSTOMER");

        loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
        loginRequest.setOrgSubdomain("testorg");

        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 86400000L);
        
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(userRoleRepository.findByRoleName(anyString())).thenReturn(Optional.of(customerRole));
    }

    @Test
    void registerUser_WithValidRequest_RegistersSuccessfully() {
        when(userRoleRepository.findByRoleName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));
        when(appUserRepository.existsByOrgIdAndEmailIgnoreCase(orgId, "new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        authService.registerUser(registrationRequest);

        verify(appUserRepository).save(any(AppUser.class));
        verify(customerRepository).save(any());
    }

    @Test
    void registerUser_WithNonCustomerRole_ThrowsException() {
        registrationRequest.setRoleName("ADMIN");

        assertThrows(RegistrationForbiddenException.class, () -> authService.registerUser(registrationRequest));
    }

    @Test
    void registerUser_WithNonExistingOrganization_ThrowsException() {
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.registerUser(registrationRequest));
    }

    @Test
    void registerUser_WithInactiveOrganization_ThrowsException() {
        organization.setIsActive(false);
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));

        assertThrows(OrganizationInactiveException.class, () -> authService.registerUser(registrationRequest));
    }

    @Test
    void registerUser_WithExistingEmail_ThrowsException() {
        when(userRoleRepository.findByRoleName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));
        when(appUserRepository.existsByOrgIdAndEmailIgnoreCase(orgId, "new@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.registerUser(registrationRequest));
    }

    @Test
    void loginUser_WithValidCredentials_ReturnsToken() {
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));
        when(appUserRepository.findByOrgIdAndEmailIgnoreCase(orgId, "test@example.com")).thenReturn(Optional.of(appUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(eq("test@example.com"), eq("CUSTOMER"), eq(orgId))).thenReturn("access-token");
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        LoginResponseDTO response = authService.loginUser(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("CUSTOMER", response.getRole());
    }

    @Test
    void loginUser_WithInactiveOrganization_ThrowsException() {
        organization.setIsActive(false);
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));

        assertThrows(OrganizationInactiveException.class, () -> authService.loginUser(loginRequest));
    }

    @Test
    void loginUser_WithInvalidPassword_ThrowsException() {
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));
        when(appUserRepository.findByOrgIdAndEmailIgnoreCase(orgId, "test@example.com")).thenReturn(Optional.of(appUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.loginUser(loginRequest));
    }

    @Test
    void loginUser_WithInactiveUser_ThrowsException() {
        appUser.setIsActive(false);
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));
        when(appUserRepository.findByOrgIdAndEmailIgnoreCase(orgId, "test@example.com")).thenReturn(Optional.of(appUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        assertThrows(AccountInactiveException.class, () -> authService.loginUser(loginRequest));
    }

    @Test
    void loginUser_WithSuperAdmin_ReturnsTokenWithNullOrgId() {
        appUser.setRole(superAdminRole);
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));
        when(appUserRepository.findByOrgIdAndEmailIgnoreCase(orgId, "test@example.com")).thenReturn(Optional.of(appUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(eq("test@example.com"), eq("SUPER_ADMIN"), isNull())).thenReturn("access-token");

        LoginResponseDTO response = authService.loginUser(loginRequest);

        assertNotNull(response);
        verify(jwtUtil).generateToken(eq("test@example.com"), eq("SUPER_ADMIN"), isNull());
    }

    @Test
    void refreshToken_WithValidToken_ReturnsNewTokens() {
        String refreshToken = "valid-refresh-token";
        String accessToken = "old-access-token";
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(refreshToken);

        when(valueOperations.get("RT:" + refreshToken)).thenReturn(userId.toString());
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(appUser));
        when(jwtUtil.generateToken(eq("test@example.com"), eq("CUSTOMER"), eq(orgId))).thenReturn("new-access-token");
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        RefreshTokenResponseDTO response = authService.refreshToken(request, accessToken);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(tokenBlacklistService).blacklistToken(accessToken);
        verify(redisTemplate).delete("RT:" + refreshToken);
    }

    @Test
    void refreshToken_WithExpiredToken_ThrowsException() {
        String refreshToken = "expired-token";
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(refreshToken);

        when(valueOperations.get("RT:" + refreshToken)).thenReturn(null);

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(request, "access-token"));
    }

    @Test
    void refreshToken_WithInactiveUser_ThrowsException() {
        String refreshToken = "valid-token";
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(refreshToken);
        appUser.setIsActive(false);

        when(valueOperations.get("RT:" + refreshToken)).thenReturn(userId.toString());
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(appUser));

        assertThrows(AccountInactiveException.class, () -> authService.refreshToken(request, "access-token"));
    }

    @Test
    void refreshToken_WithInvalidUserId_ThrowsException() {
        String refreshToken = "valid-token";
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(refreshToken);

        when(valueOperations.get("RT:" + refreshToken)).thenReturn("invalid-uuid");

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(request, "access-token"));
    }

    @Test
    void logoutUser_WithValidTokens_LogsOutSuccessfully() {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("refresh-token");

        authService.logoutUser(request, "access-token");

        verify(tokenBlacklistService).blacklistToken("access-token");
        verify(redisTemplate).delete("RT:refresh-token");
    }

    @Test
    void logoutUser_WithNullRefreshToken_StillBlacklistsAccessToken() {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(null);

        authService.logoutUser(request, "access-token");

        verify(tokenBlacklistService).blacklistToken("access-token");
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void forgotPassword_WithValidRequest_SendsEmail() {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("test@example.com");
        request.setOrgSubdomain("testorg");

        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-temp-password");

        authService.forgotPassword(request);

        verify(valueOperations).set(eq("PR:" + orgId + ":test@example.com"), eq("encoded-temp-password"),
                eq(Duration.ofMinutes(5)));
        verify(eventPublisher).publishEvent(any(EmailDispatchEvent.class));
    }

    @Test
    void forgotPassword_WithNonExistingUser_ThrowsException() {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("nonexistent@example.com");
        request.setOrgSubdomain("testorg");

        when(appUserRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.forgotPassword(request));
    }

    @Test
    void forgotPassword_WithWrongOrganization_ThrowsException() {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("test@example.com");
        request.setOrgSubdomain("wrongorg");

        Organization wrongOrg = new Organization();
        wrongOrg.setOrgId(UUID.randomUUID());
        wrongOrg.setSubdomain("wrongorg");

        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(organizationRepository.findBySubdomainIgnoreCase("wrongorg")).thenReturn(Optional.of(wrongOrg));

        assertThrows(ResourceNotFoundException.class, () -> authService.forgotPassword(request));
    }

    @Test
    void forgotPassword_WithInactiveUser_ThrowsException() {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("test@example.com");
        request.setOrgSubdomain("testorg");
        appUser.setIsActive(false);

        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));

        assertThrows(AccountInactiveException.class, () -> authService.forgotPassword(request));
    }

    @Test
    void resetPassword_WithValidRequest_ResetsPassword() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setEmail("test@example.com");
        request.setOrgSubdomain("testorg");
        request.setTemporaryPassword("temp123");
        request.setNewPassword("newPassword123");

        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));
        when(valueOperations.get("PR:" + orgId + ":test@example.com")).thenReturn("encoded-temp-password");
        when(passwordEncoder.matches("temp123", "encoded-temp-password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded-password");

        authService.resetPassword(request);

        assertEquals("new-encoded-password", appUser.getPasswordHash());
        assertTrue(appUser.getIsPasswordChanged());
        verify(appUserRepository).save(appUser);
        verify(redisTemplate).delete("PR:" + orgId + ":test@example.com");
    }

    @Test
    void resetPassword_WithInvalidTempPassword_ThrowsException() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setEmail("test@example.com");
        request.setOrgSubdomain("testorg");
        request.setTemporaryPassword("wrong-temp");

        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));
        when(valueOperations.get("PR:" + orgId + ":test@example.com")).thenReturn("encoded-temp-password");
        when(passwordEncoder.matches("wrong-temp", "encoded-temp-password")).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> authService.resetPassword(request));
    }

    @Test
    void resetPassword_WithExpiredTempPassword_ThrowsException() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setEmail("test@example.com");
        request.setOrgSubdomain("testorg");
        request.setTemporaryPassword("temp123");

        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));
        when(valueOperations.get("PR:" + orgId + ":test@example.com")).thenReturn(null);

        assertThrows(InvalidTokenException.class, () -> authService.resetPassword(request));
    }

    @Test
    void resetPassword_ClearsTenantContextAfterExecution() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setEmail("test@example.com");
        request.setOrgSubdomain("testorg");
        request.setTemporaryPassword("temp123");
        request.setNewPassword("newPassword123");

        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(organizationRepository.findBySubdomainIgnoreCase("testorg")).thenReturn(Optional.of(organization));
        when(valueOperations.get("PR:" + orgId + ":test@example.com")).thenReturn("encoded-temp-password");
        when(passwordEncoder.matches("temp123", "encoded-temp-password")).thenReturn(true);

        authService.resetPassword(request);

        assertNull(TenantContextHolder.getTenantId());
    }
}
