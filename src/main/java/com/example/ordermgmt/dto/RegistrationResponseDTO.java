package com.example.ordermgmt.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// DTO (Data Transfer Object)
// We use this class to send a response back to the user.
// Instead of sending raw text, we send a nice JSON object: { "message": "Success" }
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponseDTO {
    private String message;
}
