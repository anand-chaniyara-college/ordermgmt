package com.example.ordermgmt.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponseDTO {
    private UUID orgId;
    private String name;
    private String subdomain;
    private Boolean isActive;
    private String description;
    private LocalDateTime createdTimestamp;
}
