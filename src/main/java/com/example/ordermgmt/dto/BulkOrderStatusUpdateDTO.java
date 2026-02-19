package com.example.ordermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkOrderStatusUpdateDTO {
    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "New status is required")
    private String newStatus;
}
