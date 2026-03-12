package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.analytics.OrderAnalyticsResponseDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportResponseDTO;
import java.time.LocalDate;

public interface AdminAnalyticsService {

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
}
