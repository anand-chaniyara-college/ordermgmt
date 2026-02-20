package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk order status update dto")
public class BulkOrderStatusUpdateDTO {
    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "New status is required")
    private String newStatus;
}
