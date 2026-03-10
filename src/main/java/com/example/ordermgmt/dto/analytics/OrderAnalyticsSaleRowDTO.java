package com.example.ordermgmt.dto.analytics;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAnalyticsSaleRowDTO {
    private UUID itemId;
    private String orderStatus;
    private Long soldQty;
    private LocalDateTime soldOn;
}
