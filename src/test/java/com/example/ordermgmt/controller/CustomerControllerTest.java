package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.CustomerProfileDTO;
import com.example.ordermgmt.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerController)
                .setControllerAdvice(new com.example.ordermgmt.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("customer@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetProfile_Success() throws Exception {
        CustomerProfileDTO profileDTO = new CustomerProfileDTO();
        profileDTO.setEmail("customer@example.com");
        profileDTO.setFirstName("John");
        profileDTO.setLastName("Doe");

        when(customerService.getCustomerProfile("customer@example.com")).thenReturn(profileDTO);

        mockMvc.perform(get("/api/customer/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        verify(customerService, times(1)).getCustomerProfile("customer@example.com");
    }

    @Test
    void testUpdateProfile_Success() throws Exception {
        CustomerProfileDTO profileDTO = new CustomerProfileDTO();
        profileDTO.setFirstName("Jane");
        profileDTO.setLastName("Doe");
        profileDTO.setAddress("123 Main St");

        when(customerService.updateCustomerProfile(eq("customer@example.com"), any(CustomerProfileDTO.class)))
                .thenReturn("Profile updated successfully");

        mockMvc.perform(put("/api/customer/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));

        verify(customerService, times(1)).updateCustomerProfile(eq("customer@example.com"),
                any(CustomerProfileDTO.class));
    }

    @Test
    void testGetProfile_InternalServerError() throws Exception {
        when(customerService.getCustomerProfile("customer@example.com")).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/customer/profile"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateProfile_InternalServerError() throws Exception {
        CustomerProfileDTO profileDTO = new CustomerProfileDTO();
        profileDTO.setFirstName("Jane");
        profileDTO.setLastName("Doe");

        when(customerService.updateCustomerProfile(eq("customer@example.com"), any(CustomerProfileDTO.class)))
                .thenThrow(new RuntimeException("Error updating"));

        mockMvc.perform(put("/api/customer/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileDTO)))
                .andExpect(status().isInternalServerError());
    }
}
