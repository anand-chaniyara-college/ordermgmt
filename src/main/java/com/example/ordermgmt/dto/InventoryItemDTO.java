package com.example.ordermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemDTO {
    // Shared Data Transfer Object for both Requests (Input) and Responses (Output)

    // The unique ID of the product (e.g. "LAPTOP-001")
    private String itemId;

    // How many items are physically on the shelf ready to send
    private Integer availableStock;

    // How many items are "on hold" for orders that are being processed
    private Integer reservedStock;
}
