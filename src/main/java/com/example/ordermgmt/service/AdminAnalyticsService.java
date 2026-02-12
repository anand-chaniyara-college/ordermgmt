package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;

public interface AdminAnalyticsService {
    MonthlySalesLogDTO getMonthlyReport(String month, int year);

    void sendMonthlyReportEmail(String month, int year, String recipientEmail);
}
