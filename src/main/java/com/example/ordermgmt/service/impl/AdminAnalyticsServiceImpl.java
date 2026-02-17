package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.analytics.ItemSalesReportDTO;
import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.repository.OrderItemRepository;
import com.example.ordermgmt.service.AdminAnalyticsService;
import com.example.ordermgmt.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Month;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsServiceImpl.class);
    private final OrderItemRepository orderItemRepository;
    private final EmailService emailService;

    @Override
    public MonthlySalesLogDTO getMonthlyReport(String month, int year) {
        logger.info("Processing getMonthlyReport for month: {}, year: {}", month, year);

        int monthInt = validateAndGetMonthIndex(month);

        MonthlySalesLogDTO report = orderItemRepository.getMonthlyReport(monthInt, year);
        if (report == null || report.getTotalSoldItems() == null || report.getTotalSoldItems() == 0) {
            logger.warn("Skipping getMonthlyReport for month: {}, year: {} - No records found", month, year);
            throw new com.example.ordermgmt.exception.ResourceNotFoundException(
                    "No records found for " + month + " " + year);
        }

        List<ItemSalesReportDTO> items = orderItemRepository.getMonthlyItemWiseReport(monthInt, year);
        report.setItems(items);

        logger.info("getMonthlyReport completed successfully for month: {}, year: {}", month, year);
        return report;
    }

    @Override
    public void sendMonthlyReportEmail(String month, int year, String recipientEmail) {
        logger.info("Processing sendMonthlyReportEmail for recipient: {}, month: {}, year: {}", recipientEmail, month,
                year);
        validateAndGetMonthIndex(month);

        MonthlySalesLogDTO report = getMonthlyReport(month, year);
        String emailBody = formatSalesEmailBody(report, month, year);

        emailService.sendEmail(recipientEmail, "Monthly Sales Report: " + month + " " + year, emailBody);
        logger.info("sendMonthlyReportEmail completed successfully for recipient: {}", recipientEmail);
    }

    private int validateAndGetMonthIndex(String month) {
        try {
            return Month.valueOf(month.toUpperCase()).getValue();
        } catch (IllegalArgumentException e) {
            logger.error("Validation failed for month: {} - Invalid month name", month);
            throw new InvalidOperationException("Invalid month name: " + month);
        }
    }

    private String formatSalesEmailBody(MonthlySalesLogDTO report, String month, int year) {
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
                for (ItemSalesReportDTO item : report.getItems()) {
                    sb.append(String.format("Item ID: %s\n", item.getItemId()));
                    sb.append(String.format("Item Name: %s\n",
                            item.getItemName() != null ? item.getItemName() : "N/A"));
                    sb.append(String.format("Total Sold: %d\n", item.getTotalSoldItems()));
                    sb.append(String.format("Revenue Generated: %.2f\n", item.getTotalRevenue()));
                    sb.append("------------------------------------\n");
                }
            }
        }
        return sb.toString();
    }
}
