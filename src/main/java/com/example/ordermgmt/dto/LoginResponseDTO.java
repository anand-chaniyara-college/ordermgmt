package com.example.ordermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private String accessToken;
    private String tokenType = "Bearer";
    private String role;
    private String message;
    private String refreshToken;

    public LoginResponseDTO(String accessToken, String refreshToken, String role, String message) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
        this.message = message;
    }
}
