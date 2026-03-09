package com.example.ordermgmt.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item-wise revenue report details")
public class RevenueReportItemDTO {
    private UUID itemId;
    private String itemName;
    private BigDecimal totalRevenue;
    private Long soldQty;
    private List<String> soldOn;
}
