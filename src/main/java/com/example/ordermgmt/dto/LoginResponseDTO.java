package com.example.ordermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    // The actual "Key" the user will use for future requests
    private String accessToken;

    // "Bearer" is the standard type for JWT tokens in HTTP headers
    private String tokenType = "Bearer";

    // The role helps the Frontend decide what to show (e.g. show "Admin Dashboard"
    // button)
    private String role;

    // Simple message for the user ("Login successful")
    private String message;

    // The long-lived token used to get new access tokens
    private String refreshToken;

    // Custom constructor to make it easier to create responses in the Service
    public LoginResponseDTO(String accessToken, String refreshToken, String role, String message) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
        this.message = message;
    }
}
