package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportResponseDTO;
import java.time.LocalDate;

public interface AdminAnalyticsService {
    MonthlySalesLogDTO getMonthlyReport(String month, int year);

    RevenueReportResponseDTO getRevenueReport(
            LocalDate startDate,
            LocalDate endDate,
            String itemName,
            Integer page,
            Integer size);

    void sendMonthlyReportEmail(String month, int year, String recipientEmail);
}
