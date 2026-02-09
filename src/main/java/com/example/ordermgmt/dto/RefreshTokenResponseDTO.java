package com.example.ordermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private String message;
}
