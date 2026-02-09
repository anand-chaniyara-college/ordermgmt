package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenResponseDTO;

public interface AuthService {
    String registerUser(RegistrationRequestDTO request);

    LoginResponseDTO loginUser(LoginRequestDTO request);

    RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request);

    String logoutUser(RefreshTokenRequestDTO request);
}
