package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.CustomerProfileDTO;
import com.example.ordermgmt.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerController).build();

        // Mock Security Context
        // Mock Security Context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        org.mockito.Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        org.mockito.Mockito.lenient().when(authentication.getName()).thenReturn("test@example.com");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getProfile_Success() throws Exception {
        CustomerProfileDTO profile = new CustomerProfileDTO("John", "Doe", "1234567890", "Address", "test@example.com");
        when(customerService.getCustomerProfile("test@example.com")).thenReturn(profile);

        mockMvc.perform(get("/api/customer/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void updateProfile_Success() throws Exception {
        CustomerProfileDTO profile = new CustomerProfileDTO("John", "Doe", "1234567890", "Address", "test@example.com");
        when(customerService.updateCustomerProfile(eq("test@example.com"), any(CustomerProfileDTO.class)))
                .thenReturn("Profile updated successfully");

        mockMvc.perform(put("/api/customer/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));
    }

    @Test
    void updateProfile_ValidationError() throws Exception {
        CustomerProfileDTO invalidProfile = new CustomerProfileDTO("", "", "invalid", "", "invalid");

        mockMvc.perform(put("/api/customer/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProfile)))
                .andExpect(status().isBadRequest());
    }
}
