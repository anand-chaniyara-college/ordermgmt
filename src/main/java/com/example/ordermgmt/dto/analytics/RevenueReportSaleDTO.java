package com.example.ordermgmt.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Per-sale quantity and timestamp")
public class RevenueReportSaleDTO {
    private Long soldQty;
    private String soldOn;
}
