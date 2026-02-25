package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk order status update dto")
public class BulkOrderStatusUpdateDTO {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotBlank(message = "New status is required")
    private String newStatus;
}
