package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registration response dto")
public class RegistrationResponseDTO {
    private String message;
}
