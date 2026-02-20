package com.example.ordermgmt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer profile dto")
public class CustomerProfileDTO {

    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "First name must contain only alphabets (A-Z, a-z)")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Last name must contain only alphabets (A-Z, a-z)")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Pattern(regexp = "^\\d+$", message = "Contact number must contain only digits")
    @Size(min = 10, max = 20, message = "Contact number must be between 10 and 20 digits")
    private String contactNo;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Schema(description = "Email address (read-only, cannot be updated)")
    @Email(message = "Invalid email format")
    private String email;
}
