package com.example.ordermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: Generates Getters, Setters, toString, equals, hashcode automatically
@NoArgsConstructor // Lombok: Generates empty constructor (needed by Jackson JSON parser)
@AllArgsConstructor // Lombok: Generates constructor with all fields (useful for testing)
public class LoginRequestDTO {
    // We only need email and password to login
    private String email;
    private String password;
}
