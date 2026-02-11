package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.CustomerProfileDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    // --- getCustomerProfile Tests ---

    @Test
    @DisplayName("Get Profile Success: Should return full profile when user and customer record exist")
    void getCustomerProfile_Success() {
        // Arrange
        String email = "test@example.com";
        AppUser user = new AppUser();
        user.setEmail(email);

        Customer customer = new Customer();
        customer.setAppUser(user);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setAddress("123 Main St");
        customer.setContactNo("9876543210");

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(customerRepository.findByAppUser(user)).thenReturn(Optional.of(customer));

        // Act
        CustomerProfileDTO result = customerService.getCustomerProfile(email);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getAddress()).isEqualTo("123 Main St");
    }

    @Test
    @DisplayName("Get Profile Partial: Should return empty profile with email when customer record missing")
    void getCustomerProfile_CustomerRecordMissing() {
        // Arrange
        String email = "new@example.com";
        AppUser user = new AppUser();
        user.setEmail(email);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(customerRepository.findByAppUser(user)).thenReturn(Optional.empty());

        // Act
        CustomerProfileDTO result = customerService.getCustomerProfile(email);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getFirstName()).isNull(); // Should be empty/null
    }

    @Test
    @DisplayName("Get Profile Failure: Should throw exception when user not found")
    void getCustomerProfile_UserNotFound() {
        // Arrange
        String email = "unknown@example.com";
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.getCustomerProfile(email))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    // --- updateCustomerProfile Tests ---

    @Test
    @DisplayName("Update Profile Success: Should update existing customer record")
    void updateCustomerProfile_ExistingCustomer_Success() {
        // Arrange
        String email = "update@example.com";
        CustomerProfileDTO dto = new CustomerProfileDTO("Alan", "Walker", "1122334455", "London", email);

        AppUser user = new AppUser();
        user.setEmail(email);

        Customer existingCustomer = new Customer();
        existingCustomer.setAppUser(user);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(customerRepository.findByAppUser(user)).thenReturn(Optional.of(existingCustomer));

        // Act
        String result = customerService.updateCustomerProfile(email, dto);

        // Assert
        assertThat(result).isEqualTo("Profile updated successfully");
        assertThat(existingCustomer.getFirstName()).isEqualTo("Alan");
        assertThat(existingCustomer.getLastName()).isEqualTo("Walker");
        verify(customerRepository, times(1)).save(existingCustomer);
    }

    @Test
    @DisplayName("Update Profile Success: Should create new customer record if missing")
    void updateCustomerProfile_NewCustomer_Success() {
        // Arrange
        String email = "newuser@example.com";
        CustomerProfileDTO dto = new CustomerProfileDTO("New", "User", "0000000000", "New York", email);

        AppUser user = new AppUser();
        user.setEmail(email);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(customerRepository.findByAppUser(user)).thenReturn(Optional.empty());

        // Act
        String result = customerService.updateCustomerProfile(email, dto);

        // Assert
        assertThat(result).isEqualTo("Profile updated successfully");
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Update Profile Failure: Should throw exception when user not found")
    void updateCustomerProfile_UserNotFound() {
        // Arrange
        String email = "ghost@example.com";
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.updateCustomerProfile(email, new CustomerProfileDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }
}
