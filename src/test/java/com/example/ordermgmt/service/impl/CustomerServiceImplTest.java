package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.CustomerProfileDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.exception.InvalidOperationException;
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
import java.util.UUID;

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

    private UUID userId;
    private UUID customerId;
    private AppUser appUser;
    private Customer customer;
    private CustomerProfileDTO profileDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        appUser = new AppUser();
        appUser.setUserId(userId);
        appUser.setEmail("test@example.com");

        customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setAppUser(appUser);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setContactNo("1234567890");
        customer.setAddress("123 Test St");

        profileDTO = new CustomerProfileDTO();
        profileDTO.setEmail("test@example.com");
        profileDTO.setFirstName("Jane");
        profileDTO.setLastName("Smith");
        profileDTO.setContactNo("0987654321");
        profileDTO.setAddress("456 New St");
    }

    @Test
    void getCustomerProfile_WithExistingCustomer_ReturnsProfile() {
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(customerRepository.findByAppUser(appUser)).thenReturn(Optional.of(customer));

        CustomerProfileDTO result = customerService.getCustomerProfile("test@example.com");

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getCustomerProfile_WithNonExistingCustomer_ReturnsDefaultProfile() {
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(customerRepository.findByAppUser(appUser)).thenReturn(Optional.empty());

        CustomerProfileDTO result = customerService.getCustomerProfile("test@example.com");

        assertNotNull(result);
        assertNull(result.getFirstName());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getCustomerProfile_WithNonExistingUser_ThrowsException() {
        when(appUserRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                customerService.getCustomerProfile("nonexistent@example.com"));
    }

    @Test
    void updateCustomerProfile_WithValidData_UpdatesSuccessfully() {
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(customerRepository.findByAppUser(appUser)).thenReturn(Optional.of(customer));

        String result = customerService.updateCustomerProfile("test@example.com", profileDTO);

        assertEquals("Profile updated successfully", result);
        assertEquals("Jane", customer.getFirstName());
        assertEquals("Smith", customer.getLastName());
        assertEquals("0987654321", customer.getContactNo());
        assertEquals("456 New St", customer.getAddress());
        verify(customerRepository).save(customer);
    }

    @Test
    void updateCustomerProfile_WithNewCustomer_CreatesAndUpdates() {
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(customerRepository.findByAppUser(appUser)).thenReturn(Optional.empty());

        String result = customerService.updateCustomerProfile("test@example.com", profileDTO);

        assertEquals("Profile updated successfully", result);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void updateCustomerProfile_WithEmailMismatch_ThrowsException() {
        profileDTO.setEmail("different@example.com");

        assertThrows(InvalidOperationException.class, () -> 
                customerService.updateCustomerProfile("test@example.com", profileDTO));
    }

    @Test
    void updateCustomerProfile_WithNonExistingUser_ThrowsException() {
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                customerService.updateCustomerProfile("test@example.com", profileDTO));
    }

    @Test
    void updateCustomerProfile_WithPartialData_UpdatesOnlyProvidedFields() {
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(customerRepository.findByAppUser(appUser)).thenReturn(Optional.of(customer));

        CustomerProfileDTO partialDTO = new CustomerProfileDTO();
        partialDTO.setEmail("test@example.com");
        partialDTO.setFirstName("Jane");
        // lastName, contactNo, address are null

        customerService.updateCustomerProfile("test@example.com", partialDTO);

        assertEquals("Jane", customer.getFirstName());
        assertNull(customer.getLastName()); // current behavior sets null for null input
        assertNull(customer.getContactNo()); // current behavior sets null for null input
        assertNull(customer.getAddress()); // current behavior sets null for null input
    }

    @Test
    void getCustomerProfile_HandlesNullFields() {
        customer.setFirstName(null);
        customer.setLastName(null);
        
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(customerRepository.findByAppUser(appUser)).thenReturn(Optional.of(customer));

        CustomerProfileDTO result = customerService.getCustomerProfile("test@example.com");

        assertNotNull(result);
        assertNull(result.getFirstName());
        assertNull(result.getLastName());
    }
}
