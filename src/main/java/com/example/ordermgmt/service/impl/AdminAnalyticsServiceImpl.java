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

        java.util.List<com.example.ordermgmt.dto.analytics.ItemSalesReportDTO> items = orderItemRepository
                .getMonthlyItemWiseReport(monthInt, year);
        report.setItems(items);

        return report;
    }

    @Override
    public void sendMonthlyReportEmail(String month, int year, String recipientEmail) {
        MonthlySalesLogDTO report = getMonthlyReport(month, year);
        StringBuilder sb = new StringBuilder();

        if (report == null) {
            sb.append("No records found for ").append(month).append(" ").append(year).append(".");
        } else {
            sb.append(String.format("Monthly Sales Report for %s %d\n", month, year));
            sb.append("====================================\n\n");
            sb.append(String.format("Total Items Sold: %d\n", report.getTotalSoldItems()));
            sb.append(String.format("Total Revenue: %.2f\n\n", report.getTotalRevenue()));

            sb.append("Item-wise Breakdown:\n");
            sb.append("------------------------------------\n");

            if (report.getItems() != null) {
                for (com.example.ordermgmt.dto.analytics.ItemSalesReportDTO item : report.getItems()) {
                    sb.append(String.format("Item ID: %s\n", item.getItemId()));
                    sb.append(String.format("Item Name: %s\n",
                            item.getItemName() != null ? item.getItemName() : "N/A"));
                    sb.append(String.format("Total Sold: %d\n", item.getTotalSoldItems()));
                    sb.append(String.format("Revenue Generated: %.2f\n", item.getTotalRevenue()));
                    sb.append("------------------------------------\n");
                }
            }
        }

        emailService.sendEmail(recipientEmail, "Monthly Sales Report: " + month + " " + year, sb.toString());
    }
}
