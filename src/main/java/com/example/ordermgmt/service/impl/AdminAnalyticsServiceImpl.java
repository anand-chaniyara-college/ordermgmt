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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsServiceImpl.class);
    private final OrderItemRepository orderItemRepository;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public MonthlySalesLogDTO getMonthlyReport(String month, int year) {
        logger.info("Processing getMonthlyReport for: {}-{}", month, year);

        int monthInt = validateAndGetMonthIndex(month);

        MonthlySalesLogDTO report = orderItemRepository.getMonthlyReport(monthInt, year);
        if (report == null || report.getTotalSoldItems() == null || report.getTotalSoldItems() == 0) {
            logger.warn("Skipping getMonthlyReport for: {}-{} - No records found", month, year);
            throw new com.example.ordermgmt.exception.ResourceNotFoundException(
                    "No records found for " + month + " " + year);
        }

        List<ItemSalesReportDTO> items = orderItemRepository.getMonthlyItemWiseReport(monthInt, year);
        report.setItems(items);

        logger.info("getMonthlyReport completed successfully for: {}-{}", month, year);
        return report;
    }

    @Override
    public void sendMonthlyReportEmail(String month, int year, String recipientEmail) {
        logger.info("Processing sendMonthlyReportEmail for Admin: {}", recipientEmail);
        validateAndGetMonthIndex(month);

        MonthlySalesLogDTO report = getMonthlyReport(month, year);
        String emailBody = formatSalesEmailBody(report, month, year);

        emailService.sendEmail(recipientEmail, "Monthly Sales Report: " + month + " " + year, emailBody);
        logger.info("sendMonthlyReportEmail completed successfully for Admin: {}", recipientEmail);
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

        sb.append("<html><body>");
        sb.append("<h2 style='color: #2c3e50;'>Monthly Sales Report for ").append(month).append(" ").append(year)
                .append("</h2>");

        if (report == null) {
            sb.append("<p>No records found for ").append(month).append(" ").append(year).append(".</p>");
        } else {
            sb.append("<p><strong>Total Items Sold:</strong> ").append(report.getTotalSoldItems()).append("<br>");
            sb.append("<strong>Total Revenue:</strong> ").append(String.format("%.2f", report.getTotalRevenue()))
                    .append("</p>");

            sb.append("<h3 style='color: #2c3e50;'>Item-wise Breakdown</h3>");
            sb.append("<table style='width: 100%; border-collapse: collapse; border: 1px solid #ddd;'>");
            sb.append("<thead style='background-color: #f2f2f2;'>");
            sb.append("<tr>");
            sb.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>Item ID</th>");
            sb.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>Item Name</th>");
            sb.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: right;'>Sold</th>");
            sb.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: right;'>Revenue</th>");
            sb.append("</tr>");
            sb.append("</thead>");
            sb.append("<tbody>");

            if (report.getItems() != null) {
                for (ItemSalesReportDTO item : report.getItems()) {
                    sb.append("<tr>");
                    sb.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(item.getItemId())
                            .append("</td>");
                    sb.append("<td style='padding: 10px; border: 1px solid #ddd;'>")
                            .append(item.getItemName() != null ? item.getItemName() : "N/A").append("</td>");
                    sb.append("<td style='padding: 10px; border: 1px solid #ddd; text-align: right;'>")
                            .append(item.getTotalSoldItems()).append("</td>");
                    sb.append("<td style='padding: 10px; border: 1px solid #ddd; text-align: right;'>")
                            .append(String.format("%.2f", item.getTotalRevenue())).append("</td>");
                    sb.append("</tr>");
                }
            }
            sb.append("</tbody>");
            sb.append("</table>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }
}
