package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request dto")
public class LoginRequestDTO {
    @NotBlank(message = "Organization subdomain is required")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Organization subdomain contains invalid characters")
    private String orgSubdomain;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
