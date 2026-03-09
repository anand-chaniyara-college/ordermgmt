package com.example.ordermgmt.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Revenue report response")
public class RevenueReportResponseDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalSoldItems;
    private Long totalSoldQty;
    private BigDecimal totalRevenue;
    private List<RevenueReportItemDTO> items;
}
