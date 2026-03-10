package com.example.ordermgmt.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order analytics response")
public class OrderAnalyticsResponseDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalSoldItems;
    private Long totalSoldQty;
    private List<OrderAnalyticsItemDTO> items;
}
