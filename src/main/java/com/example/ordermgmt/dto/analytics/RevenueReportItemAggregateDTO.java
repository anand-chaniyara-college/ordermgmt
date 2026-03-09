package com.example.ordermgmt.dto.analytics;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportItemAggregateDTO {
    private UUID itemId;
    private String itemName;
    private Long soldQty;
    private BigDecimal totalRevenue;
}
