package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.analytics.ItemSoldOnRowDTO;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
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


    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponseDTO getRevenueReport(
            LocalDate startDate,
            LocalDate endDate,
            String itemName,
            org.springframework.data.domain.Pageable pageable) {
        logger.info("Processing getRevenueReport for range: {} to {}", startDate, endDate);

        validateDateRange(startDate, endDate);
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
            pagedAggregatedItems = pageable != null && pageable.isPaged()
                    ? paginateAggregates(allAggregatedItems, pageable.getPageNumber(), pageable.getPageSize())
                    : allAggregatedItems;
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
                            pageable != null ? pageable : org.springframework.data.domain.Pageable.unpaged())
                    : orderItemRepository.getRevenueReportItems(
                            startDateTime,
                            endDateTimeExclusive,
                            pageable != null ? pageable : org.springframework.data.domain.Pageable.unpaged());
            pagedAggregatedItems = itemPage != null ? itemPage.getContent() : Collections.emptyList();
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
            org.springframework.data.domain.Pageable pageable) {
        logger.info("Processing getOrderAnalytics for range: {} to {}", startDate, endDate);

        validateOptionalDateRange(startDate, endDate);
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

        List<RevenueReportItemAggregateDTO> pagedItems = pageable != null && pageable.isPaged()
                ? paginateAggregates(aggregatedItems, pageable.getPageNumber(), pageable.getPageSize())
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
        if (startDate.isAfter(endDate)) {
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

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidOperationException("Both startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidOperationException("startDate must be before endDate");
        }
    }

}
