package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk order failure dto")
public class BulkOrderFailureDTO {
    private UUID orderId;
    private String error;
}
