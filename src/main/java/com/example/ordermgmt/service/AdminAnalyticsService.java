package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;

public interface AdminAnalyticsService {
    MonthlySalesLogDTO getMonthlyReport(String month, int year);
}
