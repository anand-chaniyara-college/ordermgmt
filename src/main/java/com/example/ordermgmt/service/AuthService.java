package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.LoginRequestDTO;
import com.example.ordermgmt.dto.LoginResponseDTO;
import com.example.ordermgmt.dto.RegistrationRequestDTO;

public interface AuthService {

    // Handles the sign-up process
    // Returns a simple String message
    String registerUser(RegistrationRequestDTO request);

    // Handles the sign-in process
    // Returns a complex object containing the JWT token
    LoginResponseDTO loginUser(LoginRequestDTO request);
}
