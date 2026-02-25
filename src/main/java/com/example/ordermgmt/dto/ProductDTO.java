package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product dto")
public class ProductDTO {
    private UUID itemId;
    private String itemName;
    private BigDecimal unitPrice;
    private Integer availableStock;
}
