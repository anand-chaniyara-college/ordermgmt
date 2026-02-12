package com.example.ordermgmt.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonthlySalesLogDTO {
    private Long totalSoldItems;
    private BigDecimal totalRevenue;
    private List<ItemSalesReportDTO> items;

    public MonthlySalesLogDTO(java.lang.Long totalSoldItems, java.math.BigDecimal totalRevenue) {
        this.totalSoldItems = totalSoldItems;
        this.totalRevenue = totalRevenue;
    }
}
