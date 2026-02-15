package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenResponseDTO;

public interface AuthService {
    void registerUser(RegistrationRequestDTO request);

    LoginResponseDTO loginUser(LoginRequestDTO request);

    RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request, String accessToken);

    void logoutUser(RefreshTokenRequestDTO request, String accessToken);
}
