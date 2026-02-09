package com.example.ordermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileDTO {
    private String firstName;
    private String lastName;
    private String contactNo;
    private String email; // Included for reading, but usually not updatable via the same profile endpoint
}
