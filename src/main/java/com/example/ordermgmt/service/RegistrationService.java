package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.RegistrationRequestDTO;

// Service Interface
// Good practice to hide the actual implementation steps behind an interface.
// The Controller only talks to this Interface, not the Class directly.
public interface RegistrationService {

    // Method signature for registering a user
    // Takes the DTO (data from user) as input
    // Returns a String message ("Success" or "Error")
    String registerUser(RegistrationRequestDTO request);
}
