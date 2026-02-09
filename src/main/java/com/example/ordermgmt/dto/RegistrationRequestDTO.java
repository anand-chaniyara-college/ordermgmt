package com.example.ordermgmt.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestDTO {
    private String email;
    private String password;
    private String roleName;
}
