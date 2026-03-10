package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.dto.analytics.OrderAnalyticsResponseDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportResponseDTO;
import java.time.LocalDate;

public interface AdminAnalyticsService {
        MonthlySalesLogDTO getMonthlyReport(String month, int year);

        RevenueReportResponseDTO getRevenueReport(
                        LocalDate startDate,
                        LocalDate endDate,
                        String itemName,
                        org.springframework.data.domain.Pageable pageable);

        OrderAnalyticsResponseDTO getOrderAnalytics(
                        LocalDate startDate,
                        LocalDate endDate,
                        String itemName,
                        String orderStatus,
                        org.springframework.data.domain.Pageable pageable);

        void sendMonthlyReportEmail(String month, int year, String recipientEmail);
}
