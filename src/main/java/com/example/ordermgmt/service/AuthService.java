package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenRequestDTO;
import com.example.ordermgmt.dto.RefreshTokenResponseDTO;

public interface AuthService {

    // Handles the sign-up process
    // Returns a simple String message
    String registerUser(RegistrationRequestDTO request);

    // Handles the sign-in process
    // Returns a complex object containing the JWT token
    LoginResponseDTO loginUser(LoginRequestDTO request);

    // Handles refreshing the token
    // Returns new Access Token
    RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request);

    // Handles logout (Revokes the refresh token)
    String logoutUser(RefreshTokenRequestDTO request);
}
