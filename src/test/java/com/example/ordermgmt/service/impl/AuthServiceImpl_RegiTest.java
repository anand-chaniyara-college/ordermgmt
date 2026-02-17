package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.CustomerRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.ordermgmt.exception.UserAlreadyExistsException;
import com.example.ordermgmt.exception.RoleNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImpl_RegiTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.example.ordermgmt.service.TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Should successfully register a new CUSTOMER user and create profile")
    void registerUser_Success_Customer() {
        // Arrange
        RegistrationRequestDTO request = new RegistrationRequestDTO();
        request.setEmail("customer@test.com");
        request.setPassword("pass123");
        request.setRoleName("CUSTOMER");

        UserRole customerRole = new UserRole();
        customerRole.setRoleName("CUSTOMER");

        when(appUserRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRoleRepository.findByRoleName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");

        // Act
        // Act
        authService.registerUser(request);

        // Assert
        verify(appUserRepository, times(1)).save(any(AppUser.class));
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should successfully register a new ADMIN user without customer profile")
    void registerUser_Success_Admin() {
        // Arrange
        RegistrationRequestDTO request = new RegistrationRequestDTO();
        request.setEmail("admin@test.com");
        request.setPassword("admin123");
        request.setRoleName("ADMIN");

        UserRole adminRole = new UserRole();
        adminRole.setRoleName("ADMIN");

        when(appUserRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRoleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");

        // Act
        // Act
        authService.registerUser(request);

        // Assert
        verify(appUserRepository, times(1)).save(any(AppUser.class));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should fail registration if email already exists")
    void registerUser_Failure_EmailExists() {
        // Arrange
        RegistrationRequestDTO request = new RegistrationRequestDTO();
        request.setEmail("existing@test.com");

        when(appUserRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new AppUser()));

        // Act
        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");
        verify(appUserRepository, never()).save(any());
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail registration if role name is invalid")
    void registerUser_Failure_RoleNotFound() {
        // Arrange
        RegistrationRequestDTO request = new RegistrationRequestDTO();
        request.setEmail("new@test.com");
        request.setRoleName("MODERATOR");

        when(appUserRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRoleRepository.findByRoleName("MODERATOR")).thenReturn(Optional.empty());

        // Act
        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("Role not found");
        verify(appUserRepository, never()).save(any());
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registration with NULL request should throw exception")
    void registerUser_NullRequest() {
        assertThatThrownBy(() -> authService.registerUser(null))
                .isInstanceOf(NullPointerException.class);
    }
}
