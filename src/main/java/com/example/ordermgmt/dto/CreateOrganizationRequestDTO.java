package com.example.ordermgmt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationRequestDTO {

    @NotBlank(message = "Organization name is required")
    @Size(max = 255, message = "Organization name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Subdomain is required")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Subdomain contains invalid characters")
    @Size(max = 100, message = "Subdomain must not exceed 100 characters")
    private String subdomain;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
