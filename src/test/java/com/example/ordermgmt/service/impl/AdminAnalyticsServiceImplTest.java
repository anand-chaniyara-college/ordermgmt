// AdminAnalyticsServiceImplTest.java
package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.analytics.*;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private AdminAnalyticsServiceImpl adminAnalyticsService;

    private LocalDate startDate;
    private LocalDate endDate;
    private UUID itemId;
    private RevenueReportItemAggregateDTO aggregateItem;
    private ItemSoldOnRowDTO soldOnRow;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.of(2024, 1, 1);
        endDate = LocalDate.of(2024, 1, 31);
        itemId = UUID.randomUUID();

        aggregateItem = new RevenueReportItemAggregateDTO(itemId, "Test Item", 5L, BigDecimal.valueOf(100));
        soldOnRow = new ItemSoldOnRowDTO(itemId, 2L, LocalDateTime.now());
    }

    @Test
    void getRevenueReport_WithValidDates_ReturnsResponse() {
        when(orderItemRepository.getRevenueReportSummary(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new RevenueReportSummaryDTO(1L, 5L, BigDecimal.valueOf(100)));
        
        Page<RevenueReportItemAggregateDTO> itemPage = new PageImpl<>(List.of(aggregateItem));
        when(orderItemRepository.getRevenueReportItems(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(itemPage);
        
        when(orderItemRepository.getItemSoldOnTimestamps(any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(List.of(soldOnRow));

        RevenueReportResponseDTO response = adminAnalyticsService.getRevenueReport(
                startDate, endDate, null, PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(startDate, response.getStartDate());
        assertEquals(endDate, response.getEndDate());
        assertEquals(1L, response.getTotalSoldItems());
        assertEquals(5L, response.getTotalSoldQty());
        assertEquals(BigDecimal.valueOf(100), response.getTotalRevenue());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void getRevenueReport_WithItemNameFilter_ReturnsFilteredResponse() {
        when(orderItemRepository.getRevenueReportSummaryByItemName(
                any(LocalDateTime.class), any(LocalDateTime.class), eq("Test")))
                .thenReturn(new RevenueReportSummaryDTO(1L, 5L, BigDecimal.valueOf(100)));
        
        Page<RevenueReportItemAggregateDTO> itemPage = new PageImpl<>(List.of(aggregateItem));
        when(orderItemRepository.getRevenueReportItemsByItemName(
                any(LocalDateTime.class), any(LocalDateTime.class), eq("Test"), any(Pageable.class)))
                .thenReturn(itemPage);
        
        when(orderItemRepository.getItemSoldOnTimestampsByItemName(
                any(LocalDateTime.class), any(LocalDateTime.class), eq("Test"), anyList()))
                .thenReturn(List.of(soldOnRow));

        RevenueReportResponseDTO response = adminAnalyticsService.getRevenueReport(
                startDate, endDate, "Test", PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals("Test Item", response.getItems().getFirst().getItemName());
    }

    @Test
    void getRevenueReport_WithMultipleItemFilters_ReturnsCombinedResponse() {
        List<RevenueReportItemAggregateDTO> aggregates = List.of(
                aggregateItem,
                new RevenueReportItemAggregateDTO(UUID.randomUUID(), "Another Item", 3L, BigDecimal.valueOf(50))
        );

        when(orderItemRepository.getRevenueReportItemsList(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(aggregates);

        RevenueReportResponseDTO response = adminAnalyticsService.getRevenueReport(
                startDate, endDate, "Test,Another", PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(2, response.getItems().size());
        assertEquals(8L, response.getTotalSoldQty());
        assertEquals(BigDecimal.valueOf(150), response.getTotalRevenue());
    }

    @Test
    void getRevenueReport_WithInvalidDateRange_ThrowsException() {
        LocalDate invalidStart = LocalDate.of(2024, 2, 1);
        LocalDate invalidEnd = LocalDate.of(2024, 1, 1);

        assertThrows(InvalidOperationException.class, () ->
                adminAnalyticsService.getRevenueReport(invalidStart, invalidEnd, null, null));
    }

    @Test
    void getRevenueReport_WithNullDates_ThrowsException() {
        assertThrows(InvalidOperationException.class, () ->
                adminAnalyticsService.getRevenueReport(null, endDate, null, null));
    }

    @Test
    void getOrderAnalytics_WithAllParameters_ReturnsResponse() {
        when(orderItemRepository.getOrderAnalyticsItemsList(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(aggregateItem));
        
        when(orderItemRepository.getOrderAnalyticsSales(any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(List.of(new OrderAnalyticsSaleRowDTO(itemId, OrderStatus.DELIVERED.name(), 2L, LocalDateTime.now())));

        OrderAnalyticsResponseDTO response = adminAnalyticsService.getOrderAnalytics(
                startDate, endDate, null, null, PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1L, response.getTotalSoldItems());
        assertEquals(5L, response.getTotalSoldQty());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void getOrderAnalytics_WithOptionalNullDates_ReturnsAllData() {
        when(orderItemRepository.getOrderAnalyticsItemsListAll())
                .thenReturn(List.of(aggregateItem));
        
        when(orderItemRepository.getOrderAnalyticsSalesAll(anyList()))
                .thenReturn(List.of(new OrderAnalyticsSaleRowDTO(itemId, OrderStatus.DELIVERED.name(), 2L, LocalDateTime.now())));

        OrderAnalyticsResponseDTO response = adminAnalyticsService.getOrderAnalytics(
                null, null, null, null, PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
    }

    @Test
    void getOrderAnalytics_WithStatusFilter_ReturnsFilteredResponse() {
        List<String> statusFilters = List.of(OrderStatus.DELIVERED.name());
        
        when(orderItemRepository.getOrderAnalyticsItemsListByStatus(
                any(LocalDateTime.class), any(LocalDateTime.class), eq(statusFilters)))
                .thenReturn(List.of(aggregateItem));
        
        when(orderItemRepository.getOrderAnalyticsSalesByStatus(
                any(LocalDateTime.class), any(LocalDateTime.class), eq(statusFilters), anyList()))
                .thenReturn(List.of(new OrderAnalyticsSaleRowDTO(itemId, OrderStatus.DELIVERED.name(), 2L, LocalDateTime.now())));

        OrderAnalyticsResponseDTO response = adminAnalyticsService.getOrderAnalytics(
                startDate, endDate, null, OrderStatus.DELIVERED.name(), PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
    }

    @Test
    void getOrderAnalytics_WithCancelledAlias_WorksCorrectly() {
        when(orderItemRepository.getOrderAnalyticsItemsListByStatus(
                any(LocalDateTime.class), any(LocalDateTime.class), eq(List.of(OrderStatus.CANCELLED.name()))))
                .thenReturn(List.of(aggregateItem));

        OrderAnalyticsResponseDTO response = adminAnalyticsService.getOrderAnalytics(
                startDate, endDate, null, "CANCELED", PageRequest.of(0, 10));

        assertNotNull(response);
        verify(orderItemRepository).getOrderAnalyticsItemsListByStatus(
                any(LocalDateTime.class), any(LocalDateTime.class), eq(List.of(OrderStatus.CANCELLED.name())));
    }

    @Test
    void getOrderAnalytics_WithInvalidStatus_ThrowsException() {
        assertThrows(InvalidOperationException.class, () ->
                adminAnalyticsService.getOrderAnalytics(startDate, endDate, null, "INVALID_STATUS", PageRequest.of(0, 10)));
    }

    @Test
    void getOrderAnalytics_WithEmptyResults_ReturnsEmptyResponse() {
        when(orderItemRepository.getOrderAnalyticsItemsList(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        OrderAnalyticsResponseDTO response = adminAnalyticsService.getOrderAnalytics(
                startDate, endDate, null, null, PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(0, response.getTotalSoldItems());
        assertEquals(0, response.getTotalSoldQty());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void getOrderAnalytics_WithPagination_ReturnsPagedResults() {
        List<RevenueReportItemAggregateDTO> allItems = List.of(
                aggregateItem,
                new RevenueReportItemAggregateDTO(UUID.randomUUID(), "Item 2", 3L, BigDecimal.valueOf(50)),
                new RevenueReportItemAggregateDTO(UUID.randomUUID(), "Item 3", 4L, BigDecimal.valueOf(75))
        );

        when(orderItemRepository.getOrderAnalyticsItemsList(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(allItems);

        OrderAnalyticsResponseDTO response = adminAnalyticsService.getOrderAnalytics(
                startDate, endDate, null, null, PageRequest.of(0, 2));

        assertNotNull(response);
        assertEquals(3, response.getTotalSoldItems()); // Total count from all items
        assertEquals(2, response.getItems().size()); // Paged results
    }

    @Test
    void getRevenueReport_WithNullPageable_ReturnsAllResults() {
        List<RevenueReportItemAggregateDTO> aggregates = List.of(aggregateItem);

        when(orderItemRepository.getRevenueReportSummaryByItemName(any(LocalDateTime.class), any(LocalDateTime.class), eq("Test")))
                .thenReturn(new com.example.ordermgmt.dto.analytics.RevenueReportSummaryDTO(1L, 5L, BigDecimal.valueOf(100)));
        when(orderItemRepository.getRevenueReportItemsByItemName(any(LocalDateTime.class), any(LocalDateTime.class), eq("Test"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(aggregates));
        when(orderItemRepository.getItemSoldOnTimestampsByItemName(any(LocalDateTime.class), any(LocalDateTime.class), eq("Test"), anyList()))
                .thenReturn(List.of(soldOnRow));

        RevenueReportResponseDTO response = adminAnalyticsService.getRevenueReport(
                startDate, endDate, "Test", null);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
    }
}
