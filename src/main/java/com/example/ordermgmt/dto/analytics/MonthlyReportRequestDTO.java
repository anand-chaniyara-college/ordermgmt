package com.example.ordermgmt.dto.analytics;

import lombok.Data;

@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class MonthlyReportRequestDTO {
    private String month;
    private int year;
}
