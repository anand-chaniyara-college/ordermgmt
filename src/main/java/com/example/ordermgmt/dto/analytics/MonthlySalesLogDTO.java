package com.example.ordermgmt.dto.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonthlySalesLogDTO {
    private Long totalSoldItems;
    private BigDecimal totalRevenue;
    private List<ItemSalesReportDTO> items;

    public MonthlySalesLogDTO(Long totalSoldItems, BigDecimal totalRevenue) {
        this.totalSoldItems = totalSoldItems;
        this.totalRevenue = totalRevenue;
    }
}
