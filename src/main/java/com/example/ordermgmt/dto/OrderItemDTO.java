package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order item dto")
public class OrderItemDTO {
    @NotNull(message = "Item ID is required")
    @Schema(example = "018e4c73-1234-7abc-8def-123456789abc")
    private UUID itemId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String itemName;

    @Positive(message = "Quantity must be positive")
    @Schema(example = "2")
    private Integer quantity;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal unitPrice;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal subTotal;
}
