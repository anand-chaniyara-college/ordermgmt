package com.example.ordermgmt.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order analytics item details")
public class OrderAnalyticsItemDTO {
    private UUID itemId;
    private String itemName;
    private List<OrderAnalyticsSaleDTO> sales;
}
