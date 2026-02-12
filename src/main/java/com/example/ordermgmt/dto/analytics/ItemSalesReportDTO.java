package com.example.ordermgmt.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemSalesReportDTO {
    private String itemId;
    private String itemName;
    private Long totalSoldItems;
    private BigDecimal totalRevenue;
}
