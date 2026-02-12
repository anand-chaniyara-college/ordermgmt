package com.example.ordermgmt.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySalesLogDTO {
    private Long totalSoldItems;
    private BigDecimal totalRevenue;
}
