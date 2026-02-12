package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.repository.OrderItemRepository;
import com.example.ordermgmt.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Month;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final OrderItemRepository orderItemRepository;
    private final com.example.ordermgmt.service.EmailService emailService;

    @Override
    public MonthlySalesLogDTO getMonthlyReport(String month, int year) {
        int monthInt;
        try {
            monthInt = Month.valueOf(month.toUpperCase()).getValue();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid month name: " + month);
        }

        MonthlySalesLogDTO report = orderItemRepository.getMonthlyReport(monthInt, year);
        if (report == null || report.getTotalSoldItems() == null || report.getTotalSoldItems() == 0) {
            return null;
        }
        return report;
    }

    @Override
    public void sendMonthlyReportEmail(String month, int year, String recipientEmail) {
        MonthlySalesLogDTO report = getMonthlyReport(month, year);
        String content;
        if (report == null) {
            content = "No records found for " + month + " " + year + ".";
        } else {
            content = String.format(
                    "Monthly Sales Report for %s %d\n\nTotal Items Sold: %d\nTotal Revenue: %s",
                    month, year, report.getTotalSoldItems(), report.getTotalRevenue().toString());
        }

        emailService.sendEmail(recipientEmail, "Monthly Sales Report: " + month + " " + year, content);
    }
}
