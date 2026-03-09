package com.example.ordermgmt.dto.analytics;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportSummaryDTO {
    private Long totalSoldItems;
    private Long totalSoldQty;
    private BigDecimal totalRevenue;
}
