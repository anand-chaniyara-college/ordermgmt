package com.example.ordermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponseDTO {
    // The NEW Access Token
    private String accessToken;

    // The existing Refresh Token (sometimes we rotate it, but for now we keep it)
    private String refreshToken;

    private String tokenType = "Bearer";
    private String message;
}
