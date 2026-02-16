package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RefreshTokenRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenResponseDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.RefreshToken;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.RefreshTokenRepository;
import com.example.ordermgmt.service.TokenBlacklistService;
import com.example.ordermgmt.security.JwtUtil;
import com.example.ordermgmt.exception.InvalidCredentialsException;
import com.example.ordermgmt.exception.AccountInactiveException;
import com.example.ordermgmt.exception.InvalidTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImpl_LoginoutRefressTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    // --- LOGIN TESTS ---

    @Test
    @DisplayName("Login Success: Should return tokens when credentials are valid")
    void loginUser_Success() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@test.com");
        request.setPassword("password");

        UserRole role = new UserRole();
        role.setRoleName("CUSTOMER");

        AppUser user = new AppUser();
        user.setEmail("user@test.com");
        user.setPasswordHash("hashed_pass");
        user.setRole(role);
        user.setIsActive(true);

        RefreshToken token = new RefreshToken();
        token.setToken("fake-refresh-token");

        when(appUserRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("fake-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(token);

        // Act
        LoginResponseDTO response = authService.loginUser(request);

        // Assert
        assertThat(response.getMessage()).isEqualTo("Login successful");
        assertThat(response.getAccessToken()).isEqualTo("fake-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("fake-refresh-token");
        verify(refreshTokenRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Login Failure: Should fail when user is not found")
    void loginUser_Failure_UserNotFound() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("unknown@test.com");

        when(appUserRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // Act
        // Act & Assert
        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("Login Failure: Should fail when password matches is false")
    void loginUser_Failure_InvalidCredentials() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@test.com");
        request.setPassword("wrong-pass");

        AppUser user = new AppUser();
        user.setPasswordHash("hashed_pass");

        when(appUserRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act
        // Act & Assert
        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("Login Failure: Should fail when user is inactive")
    void loginUser_Failure_UserInactive() {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("inactive@test.com");

        AppUser user = new AppUser();
        user.setIsActive(false);

        when(appUserRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        // Act
        // Act & Assert
        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(AccountInactiveException.class)
                .hasMessage("User is inactive");
    }

    // --- REFRESH TOKEN TESTS ---

    @Test
    @DisplayName("Refresh Success: Should return new access token for valid refresh token")
    void refreshToken_Success() {
        // Arrange
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO();
        request.setRefreshToken("valid-token");

        UserRole role = new UserRole();
        role.setRoleName("CUSTOMER");

        AppUser user = new AppUser();
        user.setEmail("user@test.com");
        user.setRole(role);

        RefreshToken dbToken = new RefreshToken();
        dbToken.setAppUser(user);
        dbToken.setExpiryDate(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(dbToken));
        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(dbToken));
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("new-access-token");

        RefreshToken newToken = new RefreshToken();
        newToken.setToken("new-refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(newToken);

        // Act
        RefreshTokenResponseDTO response = authService.refreshToken(request, "dummy-access-token");

        // Assert
        assertThat(response.getMessage()).isEqualTo("Token refreshed successfully");
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(tokenBlacklistService).blacklistToken("dummy-access-token");
    }

    @Test
    @DisplayName("Refresh Failure: Should fail for expired token")
    void refreshToken_Failure_Expired() {
        // Arrange
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO();
        request.setRefreshToken("expired-token");

        RefreshToken dbToken = new RefreshToken();
        dbToken.setExpiryDate(Instant.now().minusSeconds(3600)); // Expired

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(dbToken));

        // Act
        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request, "dummy-access-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token expired");
    }

    @Test
    @DisplayName("Refresh Failure: Should throw exception when token not in DB")
    void refreshToken_Failure_NotFound() {
        // Arrange
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO();
        request.setRefreshToken("unknown-token");

        when(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request, "dummy-access-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Refresh token not found");
    }

    // --- LOGOUT TESTS ---

    @Test
    @DisplayName("Logout Success: Should revoke the refresh token")
    void logoutUser_Success() {
        // Arrange
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO();
        request.setRefreshToken("token-to-revoke");

        RefreshToken dbToken = new RefreshToken();
        dbToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(dbToken));

        // Act
        authService.logoutUser(request, "dummy-access-token");

        // Assert
        // Assert
        assertThat(dbToken.getRevoked()).isTrue();
        verify(refreshTokenRepository, times(1)).save(dbToken);
        verify(tokenBlacklistService).blacklistToken("dummy-access-token");
    }

    @Test
    @DisplayName("Login with NULL request should throw exception")
    void loginUser_NullRequest() {
        assertThatThrownBy(() -> authService.loginUser(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("RefreshToken with NULL request should throw exception")
    void refreshToken_NullRequest() {
        assertThatThrownBy(() -> authService.refreshToken(null, "dummy-access-token"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Logout Should handle case where token is not found (Idempotency)")
    void logoutUser_TokenNotFound() {
        // Arrange
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO();
        request.setRefreshToken("non-existent-token");

        when(refreshTokenRepository.findByToken("non-existent-token")).thenReturn(Optional.empty());

        // Act
        authService.logoutUser(request, "dummy-access-token");

        // Assert
        // Assert
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Refresh Should Fail when Token is Revoked")
    void refreshToken_ShouldFailForRevokedToken() {
        // Arrange
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO();
        request.setRefreshToken("revoked-token");

        UserRole role = new UserRole();
        role.setRoleName("CUSTOMER");

        AppUser user = new AppUser();
        user.setEmail("user@test.com");
        user.setRole(role);

        RefreshToken dbToken = new RefreshToken();
        dbToken.setAppUser(user);
        dbToken.setExpiryDate(Instant.now().plusSeconds(3600)); // Valid time
        dbToken.setRevoked(true); // REVOKED!

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(dbToken));

        // Act & Assert
        // This assertion checks if the service CORRECTLY rejects a revoked token.
        // It will fail if the service allows it (which is the current bug).
        assertThatThrownBy(() -> authService.refreshToken(request, "dummy-access-token"))
                .withFailMessage("Security Flaw: Service refreshed a REVOKED token!")
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("BUG FOUND: Refresh Should Fail when User is Inactive")
    void refreshToken_ShouldFailForInactiveUser() {
        // Arrange
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO();
        request.setRefreshToken("valid-token-inactive-user");

        UserRole role = new UserRole();
        role.setRoleName("CUSTOMER");

        AppUser user = new AppUser();
        user.setEmail("inactive@test.com");
        user.setRole(role);
        user.setIsActive(false); // INACTIVE USER

        RefreshToken dbToken = new RefreshToken();
        dbToken.setAppUser(user);
        dbToken.setExpiryDate(Instant.now().plusSeconds(3600));
        dbToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("valid-token-inactive-user")).thenReturn(Optional.of(dbToken));

        // Act & Assert
        // This assertion checks if the service CORRECTLY rejects an inactive user.
        assertThatThrownBy(() -> authService.refreshToken(request, "dummy-access-token"))
                .withFailMessage("Security Flaw: Service refreshed token for INACTIVE user!")
                .isInstanceOf(RuntimeException.class);
    }
}
