package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.analytics.ItemSalesReportDTO;
import com.example.ordermgmt.dto.analytics.ItemSoldOnRowDTO;
import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.dto.analytics.OrderAnalyticsItemDTO;
import com.example.ordermgmt.dto.analytics.OrderAnalyticsResponseDTO;
import com.example.ordermgmt.dto.analytics.OrderAnalyticsSaleDTO;
import com.example.ordermgmt.dto.analytics.OrderAnalyticsSaleRowDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportItemDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportResponseDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportSaleDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportSummaryDTO;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.repository.OrderItemRepository;
import com.example.ordermgmt.service.AdminAnalyticsService;
import com.example.ordermgmt.service.EmailService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final String ORDER_STATUS_ALIAS_CANCELED = "CANCELED";
    private static final String ORDER_STATUS_CANCELLED = OrderStatus.CANCELLED.name();
    private static final Set<String> VALID_ORDER_STATUSES = Arrays.stream(OrderStatus.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
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
        List<String> itemNameFilters = parseItemNameFilters(itemName);
        boolean hasItemNameFilter = !itemNameFilters.isEmpty();
        boolean isMultiFilter = itemNameFilters.size() > 1;
        String singleItemFilter = !itemNameFilters.isEmpty() ? itemNameFilters.get(0) : null;

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTimeExclusive = endDate.plusDays(1).atStartOfDay();

        RevenueReportSummaryDTO summary;
        List<RevenueReportItemAggregateDTO> pagedAggregatedItems;
        if (isMultiFilter) {
            List<RevenueReportItemAggregateDTO> allAggregatedItems = orderItemRepository
                    .getRevenueReportItemsList(startDateTime, endDateTimeExclusive)
                    .stream()
                    .filter(item -> matchesAnyFilter(item.getItemName(), itemNameFilters))
                    .sorted(Comparator.comparing(
                            RevenueReportItemAggregateDTO::getItemName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList();

            summary = buildSummaryFromAggregates(allAggregatedItems);
            pagedAggregatedItems = paginateAggregates(allAggregatedItems, pageNumber, pageSize);
        } else {
            summary = hasItemNameFilter
                    ? orderItemRepository.getRevenueReportSummaryByItemName(
                            startDateTime,
                            endDateTimeExclusive,
                            singleItemFilter)
                    : orderItemRepository.getRevenueReportSummary(
                            startDateTime,
                            endDateTimeExclusive);

            Page<RevenueReportItemAggregateDTO> itemPage = hasItemNameFilter
                    ? orderItemRepository.getRevenueReportItemsByItemName(
                            startDateTime,
                            endDateTimeExclusive,
                            singleItemFilter,
                            PageRequest.of(pageNumber, pageSize))
                    : orderItemRepository.getRevenueReportItems(
                            startDateTime,
                            endDateTimeExclusive,
                            PageRequest.of(pageNumber, pageSize));
            pagedAggregatedItems = itemPage.getContent();
        }

        List<RevenueReportItemDTO> items = buildRevenueItems(
                pagedAggregatedItems,
                startDateTime,
                endDateTimeExclusive,
                singleItemFilter,
                hasItemNameFilter && !isMultiFilter);

        logger.info("getRevenueReport completed successfully for range: {} to {}", startDate, endDate);
        return new RevenueReportResponseDTO(
                startDate,
                endDate,
                summary != null ? summary.getTotalSoldItems() : 0L,
                summary != null ? summary.getTotalSoldQty() : 0L,
                summary != null ? summary.getTotalRevenue() : BigDecimal.ZERO,
                items);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderAnalyticsResponseDTO getOrderAnalytics(
            LocalDate startDate,
            LocalDate endDate,
            String itemName,
            String orderStatus,
            Integer page,
            Integer size) {
        logger.info("Processing getOrderAnalytics for range: {} to {}", startDate, endDate);

        validateOptionalDateRange(startDate, endDate);
        PaginationRequest pagination = resolvePagination(page, size);
        List<String> itemNameFilters = parseItemNameFilters(itemName);
        boolean hasItemNameFilter = !itemNameFilters.isEmpty();
        boolean isMultiItemFilter = itemNameFilters.size() > 1;
        String singleItemFilter = hasItemNameFilter ? itemNameFilters.get(0) : null;

        List<String> orderStatusFilters = parseOrderStatusFilters(orderStatus);
        boolean hasStatusFilter = !orderStatusFilters.isEmpty();

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTimeExclusive = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        List<RevenueReportItemAggregateDTO> aggregatedItems;
        if (isMultiItemFilter) {
            List<RevenueReportItemAggregateDTO> baseItems = fetchOrderAnalyticsItemsList(
                    startDateTime,
                    endDateTimeExclusive,
                    orderStatusFilters,
                    hasStatusFilter);

            aggregatedItems = baseItems.stream()
                    .filter(item -> matchesAnyFilter(item.getItemName(), itemNameFilters))
                    .sorted(Comparator.comparing(
                            RevenueReportItemAggregateDTO::getItemName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList();
        } else if (hasItemNameFilter) {
            aggregatedItems = fetchOrderAnalyticsItemsByItemName(
                    startDateTime,
                    endDateTimeExclusive,
                    singleItemFilter,
                    orderStatusFilters,
                    hasStatusFilter);
        } else {
            aggregatedItems = fetchOrderAnalyticsItemsList(
                    startDateTime,
                    endDateTimeExclusive,
                    orderStatusFilters,
                    hasStatusFilter);
        }

        long totalSoldItems = aggregatedItems.size();
        long totalSoldQty = aggregatedItems.stream()
                .map(RevenueReportItemAggregateDTO::getSoldQty)
                .filter(qty -> qty != null)
                .mapToLong(Long::longValue)
                .sum();

        List<RevenueReportItemAggregateDTO> pagedItems = pagination.enabled()
                ? paginateAggregates(aggregatedItems, pagination.page(), pagination.size())
                : aggregatedItems;

        Map<UUID, List<OrderAnalyticsSaleDTO>> salesMap = buildOrderAnalyticsSalesMap(
                pagedItems,
                startDateTime,
                endDateTimeExclusive,
                orderStatusFilters,
                hasStatusFilter);

        List<OrderAnalyticsItemDTO> items = pagedItems.stream()
                .map(aggregatedItem -> new OrderAnalyticsItemDTO(
                        aggregatedItem.getItemId(),
                        aggregatedItem.getItemName(),
                        salesMap.getOrDefault(aggregatedItem.getItemId(), List.of())))
                .toList();

        logger.info("getOrderAnalytics completed successfully for range: {} to {}", startDate, endDate);
        return new OrderAnalyticsResponseDTO(
                startDate,
                endDate,
                totalSoldItems,
                totalSoldQty,
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

        Map<UUID, List<RevenueReportSaleDTO>> salesMap = (hasItemNameFilter
                ? orderItemRepository.getItemSoldOnTimestampsByItemName(
                        startDateTime, endDateTimeExclusive, itemName, itemIds)
                : orderItemRepository.getItemSoldOnTimestamps(
                        startDateTime, endDateTimeExclusive, itemIds))
                        .stream()
                        .collect(Collectors.groupingBy(
                                ItemSoldOnRowDTO::getItemId,
                                Collectors.mapping(
                                        this::toRevenueSale,
                                        Collectors.toList())));

        List<RevenueReportItemDTO> items = new ArrayList<>(aggregatedItems.size());
        for (RevenueReportItemAggregateDTO aggregatedItem : aggregatedItems) {
            items.add(new RevenueReportItemDTO(
                    aggregatedItem.getItemId(),
                    aggregatedItem.getItemName(),
                    aggregatedItem.getTotalRevenue(),
                    salesMap.getOrDefault(aggregatedItem.getItemId(), List.of())));
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

    private List<String> parseItemNameFilters(String itemName) {
        String normalized = normalizeItemName(itemName);
        if (normalized == null) {
            return List.of();
        }
        return List.of(normalized.split(","))
                .stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private List<String> parseOrderStatusFilters(String orderStatus) {
        if (orderStatus == null || orderStatus.isBlank()) {
            return List.of();
        }
        return List.of(orderStatus.split(","))
                .stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::normalizeOrderStatus)
                .distinct()
                .toList();
    }

    private boolean matchesAnyFilter(String itemName, List<String> filters) {
        if (itemName == null || filters == null || filters.isEmpty()) {
            return false;
        }
        String candidate = itemName.toLowerCase();
        for (String filter : filters) {
            if (candidate.contains(filter.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private RevenueReportSummaryDTO buildSummaryFromAggregates(List<RevenueReportItemAggregateDTO> items) {
        if (items == null || items.isEmpty()) {
            return new RevenueReportSummaryDTO(0L, 0L, BigDecimal.ZERO);
        }

        long totalSoldItems = items.size();
        long totalSoldQty = items.stream()
                .map(RevenueReportItemAggregateDTO::getSoldQty)
                .filter(qty -> qty != null)
                .mapToLong(Long::longValue)
                .sum();
        BigDecimal totalRevenue = items.stream()
                .map(RevenueReportItemAggregateDTO::getTotalRevenue)
                .filter(revenue -> revenue != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RevenueReportSummaryDTO(totalSoldItems, totalSoldQty, totalRevenue);
    }

    private List<RevenueReportItemAggregateDTO> paginateAggregates(
            List<RevenueReportItemAggregateDTO> allItems,
            int page,
            int size) {
        if (allItems == null || allItems.isEmpty()) {
            return List.of();
        }
        int fromIndex = page * size;
        if (fromIndex >= allItems.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, allItems.size());
        return allItems.subList(fromIndex, toIndex);
    }

    private String formatSoldOn(LocalDateTime timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.atOffset(ZoneOffset.UTC).format(SOLD_ON_FORMATTER);
    }

    private RevenueReportSaleDTO toRevenueSale(ItemSoldOnRowDTO row) {
        return new RevenueReportSaleDTO(
                row.getSoldQty() != null ? row.getSoldQty() : 0L,
                formatSoldOn(row.getSoldOn()));
    }

    private Map<UUID, List<OrderAnalyticsSaleDTO>> buildOrderAnalyticsSalesMap(
            List<RevenueReportItemAggregateDTO> aggregatedItems,
            LocalDateTime startDateTime,
            LocalDateTime endDateTimeExclusive,
            List<String> orderStatusFilters,
            boolean hasStatusFilter) {
        if (aggregatedItems == null || aggregatedItems.isEmpty()) {
            return Map.of();
        }
        List<UUID> itemIds = aggregatedItems.stream()
                .map(RevenueReportItemAggregateDTO::getItemId)
                .toList();

        List<OrderAnalyticsSaleRowDTO> rows = fetchOrderAnalyticsSalesRows(
                startDateTime,
                endDateTimeExclusive,
                orderStatusFilters,
                hasStatusFilter,
                itemIds);

        return rows.stream()
                .collect(Collectors.groupingBy(
                        OrderAnalyticsSaleRowDTO::getItemId,
                        Collectors.mapping(
                                this::toOrderAnalyticsSale,
                                Collectors.toList())));
    }

    private List<OrderAnalyticsSaleRowDTO> fetchOrderAnalyticsSalesRows(
            LocalDateTime startDateTime,
            LocalDateTime endDateTimeExclusive,
            List<String> orderStatusFilters,
            boolean hasStatusFilter,
            List<UUID> itemIds) {
        if (startDateTime == null || endDateTimeExclusive == null) {
            return hasStatusFilter
                    ? orderItemRepository.getOrderAnalyticsSalesByStatusAll(orderStatusFilters, itemIds)
                    : orderItemRepository.getOrderAnalyticsSalesAll(itemIds);
        }
        return hasStatusFilter
                ? orderItemRepository.getOrderAnalyticsSalesByStatus(
                        startDateTime, endDateTimeExclusive, orderStatusFilters, itemIds)
                : orderItemRepository.getOrderAnalyticsSales(
                        startDateTime, endDateTimeExclusive, itemIds);
    }

    private OrderAnalyticsSaleDTO toOrderAnalyticsSale(OrderAnalyticsSaleRowDTO row) {
        return new OrderAnalyticsSaleDTO(
                row.getOrderStatus(),
                row.getSoldQty() != null ? row.getSoldQty() : 0L,
                formatSoldOn(row.getSoldOn()));
    }

    private void validateOptionalDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return;
        }
        if (startDate == null || endDate == null) {
            throw new InvalidOperationException("Both startDate and endDate are required when filtering by date");
        }
        if (!startDate.isBefore(endDate)) {
            throw new InvalidOperationException("startDate must be before endDate");
        }
    }

    private String normalizeOrderStatus(String rawStatus) {
        String candidate = rawStatus.toUpperCase();
        if (ORDER_STATUS_ALIAS_CANCELED.equals(candidate)) {
            candidate = ORDER_STATUS_CANCELLED;
        }
        if (!VALID_ORDER_STATUSES.contains(candidate)) {
            throw new InvalidOperationException("Invalid orderStatus: " + rawStatus);
        }
        return candidate;
    }

    private List<RevenueReportItemAggregateDTO> fetchOrderAnalyticsItemsList(
            LocalDateTime startDateTime,
            LocalDateTime endDateTimeExclusive,
            List<String> orderStatusFilters,
            boolean hasStatusFilter) {
        if (startDateTime == null || endDateTimeExclusive == null) {
            return hasStatusFilter
                    ? orderItemRepository.getOrderAnalyticsItemsListByStatusAll(orderStatusFilters)
                    : orderItemRepository.getOrderAnalyticsItemsListAll();
        }
        return hasStatusFilter
                ? orderItemRepository.getOrderAnalyticsItemsListByStatus(
                        startDateTime, endDateTimeExclusive, orderStatusFilters)
                : orderItemRepository.getOrderAnalyticsItemsList(
                        startDateTime, endDateTimeExclusive);
    }

    private List<RevenueReportItemAggregateDTO> fetchOrderAnalyticsItemsByItemName(
            LocalDateTime startDateTime,
            LocalDateTime endDateTimeExclusive,
            String itemName,
            List<String> orderStatusFilters,
            boolean hasStatusFilter) {
        if (startDateTime == null || endDateTimeExclusive == null) {
            return hasStatusFilter
                    ? orderItemRepository.getOrderAnalyticsItemsByItemNameAndStatusAll(itemName, orderStatusFilters)
                    : orderItemRepository.getOrderAnalyticsItemsByItemNameAll(itemName);
        }
        return hasStatusFilter
                ? orderItemRepository.getOrderAnalyticsItemsByItemNameAndStatus(
                        startDateTime, endDateTimeExclusive, itemName, orderStatusFilters)
                : orderItemRepository.getOrderAnalyticsItemsByItemName(
                        startDateTime, endDateTimeExclusive, itemName);
    }

    private PaginationRequest resolvePagination(Integer page, Integer size) {
        if (page != null && page < 0) {
            throw new InvalidOperationException("page must be 0 or greater");
        }
        if (size != null && size <= 0) {
            throw new InvalidOperationException("size must be greater than 0");
        }
        if (page != null && size != null) {
            return new PaginationRequest(true, page, size);
        }
        return new PaginationRequest(false, 0, 0);
    }

    private record PaginationRequest(boolean enabled, int page, int size) {
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
