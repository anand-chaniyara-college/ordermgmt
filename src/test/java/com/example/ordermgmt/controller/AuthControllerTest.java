package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.*;
import com.example.ordermgmt.service.AuthService;
import com.example.ordermgmt.service.RateLimitingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private RateLimitingService rateLimitingService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new com.example.ordermgmt.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(authController, "rateLimitRequests", 1);
        ReflectionTestUtils.setField(authController, "rateLimitWindowSeconds", 60);
    }

    @Test
    void testRegister_Success() throws Exception {
        RegistrationRequestDTO requestDTO = new RegistrationRequestDTO();
        requestDTO.setEmail("test@example.com");
        requestDTO.setPassword("password123");
        requestDTO.setRoleName("CUSTOMER");
        requestDTO.setOrgSubdomain("test");

        when(rateLimitingService.allowRequest(anyString(), anyString(), anyLong(), anyLong())).thenReturn(true);
        doNothing().when(authService).registerUser(any(RegistrationRequestDTO.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"));

        verify(authService, times(1)).registerUser(any(RegistrationRequestDTO.class));
    }

    @Test
    void testRegister_RateLimitExceeded() throws Exception {
        RegistrationRequestDTO requestDTO = new RegistrationRequestDTO();
        requestDTO.setEmail("test@example.com");
        requestDTO.setPassword("password123");
        requestDTO.setRoleName("CUSTOMER");
        requestDTO.setOrgSubdomain("test");

        when(rateLimitingService.allowRequest(anyString(), anyString(), anyLong(), anyLong())).thenReturn(false);

        mockMvc.perform(post("/api/auth/register")
                .header("X-Forwarded-For", "192.168.1.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many registration attempts. Please try again later."));

        verify(authService, never()).registerUser(any());
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setOrgSubdomain("test");
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        LoginResponseDTO loginResponse = new LoginResponseDTO();
        loginResponse.setAccessToken("access.token.string");

        when(authService.loginUser(any(LoginRequestDTO.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.token.string"));

        verify(authService, times(1)).loginUser(any(LoginRequestDTO.class));
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO();
        refreshRequest.setRefreshToken("some-refresh-token");
        RefreshTokenResponseDTO refreshResponse = new RefreshTokenResponseDTO();
        refreshResponse.setAccessToken("new.access.token");

        when(authService.refreshToken(any(RefreshTokenRequestDTO.class), anyString())).thenReturn(refreshResponse);

        mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer old.access.token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"));

        verify(authService, times(1)).refreshToken(any(RefreshTokenRequestDTO.class), eq("old.access.token"));
    }

    @Test
    void testLogout_Success() throws Exception {
        RefreshTokenRequestDTO logoutRequest = new RefreshTokenRequestDTO();
        logoutRequest.setRefreshToken("some-refresh-token");

        doNothing().when(authService).logoutUser(any(RefreshTokenRequestDTO.class), anyString());

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer access.token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService, times(1)).logoutUser(any(RefreshTokenRequestDTO.class), eq("access.token"));
    }

    @Test
    void testRegister_InternalServerError() throws Exception {
        RegistrationRequestDTO requestDTO = new RegistrationRequestDTO();
        requestDTO.setEmail("test@example.com");
        requestDTO.setPassword("password123");
        requestDTO.setRoleName("CUSTOMER");
        requestDTO.setOrgSubdomain("test");

        when(rateLimitingService.allowRequest(anyString(), anyString(), anyLong(), anyLong())).thenReturn(true);
        doThrow(new RuntimeException("DB error")).when(authService).registerUser(any(RegistrationRequestDTO.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testLogin_InternalServerError() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setOrgSubdomain("test");
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        when(authService.loginUser(any(LoginRequestDTO.class))).thenThrow(new RuntimeException("Service failure"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError());
    }
}
