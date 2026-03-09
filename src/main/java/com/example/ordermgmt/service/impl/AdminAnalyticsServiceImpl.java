package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.analytics.ItemSalesReportDTO;
import com.example.ordermgmt.dto.analytics.ItemSoldOnRowDTO;
import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportItemDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportResponseDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportSummaryDTO;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.repository.OrderItemRepository;
import com.example.ordermgmt.service.AdminAnalyticsService;
import com.example.ordermgmt.service.EmailService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsServiceImpl.class);
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final DateTimeFormatter SOLD_ON_FORMATTER = DateTimeFormatter.ISO_INSTANT;
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
    @Transactional(readOnly = true)
    public RevenueReportResponseDTO getRevenueReport(
            LocalDate startDate,
            LocalDate endDate,
            String itemName,
            Integer page,
            Integer size) {
        logger.info("Processing getRevenueReport for range: {} to {}", startDate, endDate);

        validateDateRange(startDate, endDate);
        int pageNumber = resolvePage(page);
        int pageSize = resolveSize(size);
        String normalizedItemName = normalizeItemName(itemName);
        boolean hasItemNameFilter = normalizedItemName != null;

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTimeExclusive = endDate.plusDays(1).atStartOfDay();

        RevenueReportSummaryDTO summary = hasItemNameFilter
                ? orderItemRepository.getRevenueReportSummaryByItemName(
                        startDateTime,
                        endDateTimeExclusive,
                        normalizedItemName)
                : orderItemRepository.getRevenueReportSummary(
                        startDateTime,
                        endDateTimeExclusive);

        Page<RevenueReportItemAggregateDTO> itemPage = hasItemNameFilter
                ? orderItemRepository.getRevenueReportItemsByItemName(
                        startDateTime,
                        endDateTimeExclusive,
                        normalizedItemName,
                        PageRequest.of(pageNumber, pageSize))
                : orderItemRepository.getRevenueReportItems(
                        startDateTime,
                        endDateTimeExclusive,
                        PageRequest.of(pageNumber, pageSize));

        List<RevenueReportItemDTO> items = buildRevenueItems(
                itemPage.getContent(),
                startDateTime,
                endDateTimeExclusive,
                normalizedItemName,
                hasItemNameFilter);

        logger.info("getRevenueReport completed successfully for range: {} to {}", startDate, endDate);
        return new RevenueReportResponseDTO(
                startDate,
                endDate,
                summary != null ? summary.getTotalSoldItems() : 0L,
                summary != null ? summary.getTotalSoldQty() : 0L,
                summary != null ? summary.getTotalRevenue() : java.math.BigDecimal.ZERO,
                items);
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

    private List<RevenueReportItemDTO> buildRevenueItems(
            List<RevenueReportItemAggregateDTO> aggregatedItems,
            LocalDateTime startDateTime,
            LocalDateTime endDateTimeExclusive,
            String itemName,
            boolean hasItemNameFilter) {
        if (aggregatedItems == null || aggregatedItems.isEmpty()) {
            return List.of();
        }

        List<UUID> itemIds = aggregatedItems.stream()
                .map(RevenueReportItemAggregateDTO::getItemId)
                .toList();

        Map<UUID, LinkedHashSet<String>> soldOnMap = (hasItemNameFilter
                ? orderItemRepository.getItemSoldOnTimestampsByItemName(
                        startDateTime, endDateTimeExclusive, itemName, itemIds)
                : orderItemRepository.getItemSoldOnTimestamps(
                        startDateTime, endDateTimeExclusive, itemIds))
                .stream()
                .collect(Collectors.groupingBy(
                        ItemSoldOnRowDTO::getItemId,
                        Collectors.mapping(
                                row -> formatSoldOn(row.getSoldOn()),
                                Collectors.toCollection(LinkedHashSet::new))));

        List<RevenueReportItemDTO> items = new ArrayList<>(aggregatedItems.size());
        for (RevenueReportItemAggregateDTO aggregatedItem : aggregatedItems) {
            LinkedHashSet<String> soldOn = soldOnMap.getOrDefault(aggregatedItem.getItemId(), new LinkedHashSet<>());
            items.add(new RevenueReportItemDTO(
                    aggregatedItem.getItemId(),
                    aggregatedItem.getItemName(),
                    aggregatedItem.getTotalRevenue(),
                    aggregatedItem.getSoldQty(),
                    new ArrayList<>(soldOn)));
        }
        return items;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidOperationException("Both startDate and endDate are required");
        }
        if (!startDate.isBefore(endDate)) {
            throw new InvalidOperationException("startDate must be before endDate");
        }
    }

    private int resolvePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 0) {
            throw new InvalidOperationException("page must be 0 or greater");
        }
        return page;
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size <= 0) {
            throw new InvalidOperationException("size must be greater than 0");
        }
        return size;
    }

    private String normalizeItemName(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return null;
        }
        return itemName.trim();
    }

    private String formatSoldOn(LocalDateTime timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.atOffset(ZoneOffset.UTC).format(SOLD_ON_FORMATTER);
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
