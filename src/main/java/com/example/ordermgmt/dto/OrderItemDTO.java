package com.example.ordermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    @NotBlank(message = "Item ID is required")
    private String itemId;

    private String itemName;

    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}
