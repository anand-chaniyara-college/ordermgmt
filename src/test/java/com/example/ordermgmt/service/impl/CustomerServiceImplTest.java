package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.CustomerProfileDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.exception.ResourceNotFoundException;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    private AppUser mockUser;
    private Customer mockCustomer;
    private CustomerProfileDTO mockProfileDTO;

    @BeforeEach
    void setUp() {
        mockUser = new AppUser();
        mockUser.setUserId("user-123");
        mockUser.setEmail("test@example.com");

        mockCustomer = new Customer();
        mockCustomer.setCustomerId("cust-123");
        mockCustomer.setAppUser(mockUser);
        mockCustomer.setFirstName("John");
        mockCustomer.setLastName("Doe");
        mockCustomer.setContactNo("1234567890");
        mockCustomer.setAddress("123 Main St");

        mockProfileDTO = new CustomerProfileDTO("Jane", "Doe", "0987654321", "456 Elm St", "test@example.com");
    }

    @Test
    void getCustomerProfile_Success() {
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(customerRepository.findByAppUser(mockUser)).thenReturn(Optional.of(mockCustomer));

        CustomerProfileDTO result = customerService.getCustomerProfile("test@example.com");

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getCustomerProfile_UserNotFound_ThrowsException() {
        when(appUserRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomerProfile("unknown@example.com"));
    }

    @Test
    void getCustomerProfile_CustomerNotFound_ReturnsDefault() {
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(customerRepository.findByAppUser(mockUser)).thenReturn(Optional.empty());

        CustomerProfileDTO result = customerService.getCustomerProfile("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertNull(result.getFirstName());
    }

    @Test
    void updateCustomerProfile_Success_ExistingCustomer() {
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(customerRepository.findByAppUser(mockUser)).thenReturn(Optional.of(mockCustomer));

        String result = customerService.updateCustomerProfile("test@example.com", mockProfileDTO);

        assertEquals("Profile updated successfully", result);
        verify(customerRepository, times(1)).save(any(Customer.class));
        assertEquals("Jane", mockCustomer.getFirstName());
    }

    @Test
    void updateCustomerProfile_Success_NewCustomer() {
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(customerRepository.findByAppUser(mockUser)).thenReturn(Optional.empty());

        String result = customerService.updateCustomerProfile("test@example.com", mockProfileDTO);

        assertEquals("Profile updated successfully", result);
        verify(customerRepository, times(1)).save(any(Customer.class));
    }
}
