package com.example.ordermgmt.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item sales report dto")
public class ItemSalesReportDTO {
    private UUID itemId;
    private String itemName;
    private Long totalSoldItems;
    private BigDecimal totalRevenue;
}
