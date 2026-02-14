package com.example.ordermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemDTO {
    private String itemId;
    private String itemName;
    private Integer availableStock;
    private Integer reservedStock;
    private java.math.BigDecimal currentPrice;
}
