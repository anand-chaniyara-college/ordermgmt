package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refresh token response dto")
public class RefreshTokenResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private String message;
}
