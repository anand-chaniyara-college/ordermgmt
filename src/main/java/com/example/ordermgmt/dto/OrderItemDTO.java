package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order item dto")
public class OrderItemDTO {
    @NotBlank(message = "Item ID is required")
    @Schema(example = "SKU-1234")
    private String itemId;

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
