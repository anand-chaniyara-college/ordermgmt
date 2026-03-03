package com.example.ordermgmt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequestDTO {
    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
