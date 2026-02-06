package com.example.ordermgmt.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// DTO (Data Transfer Object)
// We use this class to receive data from the user (Frontend/Postman)
// It only contains the fields we expect the user to send during registration.
@Data // Generates Getters, Setters, toString, etc.
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestDTO {
    private String email;
    private String password;
    private String roleName;
}
