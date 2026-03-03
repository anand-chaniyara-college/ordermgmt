package com.example.ordermgmt.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private UUID userId;
    private String email;
    private String role;
    private UUID orgId;
    private Boolean isActive;
    private LocalDateTime createdTimestamp;
    private String message;
}
