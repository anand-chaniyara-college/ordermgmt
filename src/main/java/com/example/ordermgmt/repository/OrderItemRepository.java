package com.example.ordermgmt.repository;

import com.example.ordermgmt.dto.analytics.ItemSoldOnRowDTO;
import com.example.ordermgmt.dto.analytics.ItemSalesReportDTO;
import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.dto.analytics.OrderAnalyticsSaleRowDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportSummaryDTO;
import com.example.ordermgmt.entity.OrderItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItem.OrderItemId> {
        List<OrderItem> findByOrderOrderId(UUID orderId);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO("
                        +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE FUNCTION('date_part', 'year', o.createdTimestamp) = :year " +
                        "AND FUNCTION('date_part', 'month', o.createdTimestamp) = :month " +
                        "AND o.status.statusName = 'DELIVERED'")
        MonthlySalesLogDTO getMonthlyReport(
                        @Param("month") int month,
                        @Param("year") int year);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.ItemSalesReportDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE FUNCTION('date_part', 'year', o.createdTimestamp) = :year " +
                        "AND FUNCTION('date_part', 'month', o.createdTimestamp) = :month " +
                        "AND o.status.statusName = 'DELIVERED' " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName")
        List<ItemSalesReportDTO> getMonthlyItemWiseReport(
                        @Param("month") int month,
                        @Param("year") int year);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.RevenueReportSummaryDTO("
                        +
                        "COUNT(DISTINCT oi.inventoryItem.itemId), " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "AND o.status.statusName = 'DELIVERED'")
        RevenueReportSummaryDTO getRevenueReportSummary(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.RevenueReportSummaryDTO("
                        +
                        "COUNT(DISTINCT oi.inventoryItem.itemId), " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "AND o.status.statusName = 'DELIVERED' " +
                        "AND LOWER(oi.inventoryItem.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))")
        RevenueReportSummaryDTO getRevenueReportSummaryByItemName(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                        @Param("itemName") String itemName);

        @Query(value = "SELECT new com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "AND o.status.statusName = 'DELIVERED' " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName " +
                        "ORDER BY oi.inventoryItem.itemName ASC", countQuery = "SELECT COUNT(DISTINCT oi.inventoryItem.itemId) "
                                        +
                                        "FROM OrderItem oi " +
                                        "JOIN oi.order o " +
                                        "WHERE o.createdTimestamp >= :startDateTime " +
                                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                                        "AND o.status.statusName = 'DELIVERED'")
        Page<RevenueReportItemAggregateDTO> getRevenueReportItems(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                        Pageable pageable);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "AND o.status.statusName = 'DELIVERED' " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName " +
                        "ORDER BY oi.inventoryItem.itemName ASC")
        List<RevenueReportItemAggregateDTO> getRevenueReportItemsList(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive);

        @Query(value = "SELECT new com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "AND o.status.statusName = 'DELIVERED' " +
                        "AND LOWER(oi.inventoryItem.itemName) LIKE LOWER(CONCAT('%', :itemName, '%')) " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName " +
                        "ORDER BY oi.inventoryItem.itemName ASC", countQuery = "SELECT COUNT(DISTINCT oi.inventoryItem.itemId) "
                                        +
                                        "FROM OrderItem oi " +
                                        "JOIN oi.order o " +
                                        "WHERE o.createdTimestamp >= :startDateTime " +
                                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                                        "AND o.status.statusName = 'DELIVERED' " +
                                        "AND LOWER(oi.inventoryItem.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))")
        Page<RevenueReportItemAggregateDTO> getRevenueReportItemsByItemName(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                        @Param("itemName") String itemName,
                        Pageable pageable);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.ItemSoldOnRowDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "o.createdTimestamp) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "AND o.status.statusName = 'DELIVERED' " +
                        "AND oi.inventoryItem.itemId IN :itemIds " +
                        "GROUP BY oi.inventoryItem.itemId, o.createdTimestamp " +
                        "ORDER BY o.createdTimestamp ASC")
        List<ItemSoldOnRowDTO> getItemSoldOnTimestamps(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                        @Param("itemIds") List<UUID> itemIds);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.ItemSoldOnRowDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "o.createdTimestamp) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "AND o.status.statusName = 'DELIVERED' " +
                        "AND LOWER(oi.inventoryItem.itemName) LIKE LOWER(CONCAT('%', :itemName, '%')) " +
                        "AND oi.inventoryItem.itemId IN :itemIds " +
                        "GROUP BY oi.inventoryItem.itemId, o.createdTimestamp " +
                        "ORDER BY o.createdTimestamp ASC")
        List<ItemSoldOnRowDTO> getItemSoldOnTimestampsByItemName(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                        @Param("itemName") String itemName,
                        @Param("itemIds") List<UUID> itemIds);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName " +
                        "ORDER BY oi.inventoryItem.itemName ASC")
        List<RevenueReportItemAggregateDTO> getOrderAnalyticsItemsList(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "AND UPPER(o.status.statusName) IN :statuses " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName " +
                        "ORDER BY oi.inventoryItem.itemName ASC")
        List<RevenueReportItemAggregateDTO> getOrderAnalyticsItemsListByStatus(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                        @Param("statuses") List<String> statuses);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "AND LOWER(oi.inventoryItem.itemName) LIKE LOWER(CONCAT('%', :itemName, '%')) " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName " +
                        "ORDER BY oi.inventoryItem.itemName ASC")
        List<RevenueReportItemAggregateDTO> getOrderAnalyticsItemsByItemName(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                        @Param("itemName") String itemName);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "AND UPPER(o.status.statusName) IN :statuses " +
                        "AND LOWER(oi.inventoryItem.itemName) LIKE LOWER(CONCAT('%', :itemName, '%')) " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName " +
                        "ORDER BY oi.inventoryItem.itemName ASC")
        List<RevenueReportItemAggregateDTO> getOrderAnalyticsItemsByItemNameAndStatus(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                        @Param("itemName") String itemName,
                        @Param("statuses") List<String> statuses);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.OrderAnalyticsSaleRowDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "UPPER(o.status.statusName), " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "o.createdTimestamp) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "AND oi.inventoryItem.itemId IN :itemIds " +
                        "GROUP BY oi.inventoryItem.itemId, o.status.statusName, o.createdTimestamp " +
                        "ORDER BY o.createdTimestamp ASC")
        List<OrderAnalyticsSaleRowDTO> getOrderAnalyticsSales(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                        @Param("itemIds") List<UUID> itemIds);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.OrderAnalyticsSaleRowDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "UPPER(o.status.statusName), " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "o.createdTimestamp) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE o.createdTimestamp >= :startDateTime " +
                        "AND o.createdTimestamp < :endDateTimeExclusive " +
                        "AND UPPER(o.status.statusName) IN :statuses " +
                        "AND oi.inventoryItem.itemId IN :itemIds " +
                        "GROUP BY oi.inventoryItem.itemId, o.status.statusName, o.createdTimestamp " +
                        "ORDER BY o.createdTimestamp ASC")
        List<OrderAnalyticsSaleRowDTO> getOrderAnalyticsSalesByStatus(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTimeExclusive") LocalDateTime endDateTimeExclusive,
                        @Param("statuses") List<String> statuses,
                        @Param("itemIds") List<UUID> itemIds);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName " +
                        "ORDER BY oi.inventoryItem.itemName ASC")
        List<RevenueReportItemAggregateDTO> getOrderAnalyticsItemsListAll();

        @Query("SELECT new com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE UPPER(o.status.statusName) IN :statuses " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName " +
                        "ORDER BY oi.inventoryItem.itemName ASC")
        List<RevenueReportItemAggregateDTO> getOrderAnalyticsItemsListByStatusAll(
                        @Param("statuses") List<String> statuses);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE LOWER(oi.inventoryItem.itemName) LIKE LOWER(CONCAT('%', :itemName, '%')) " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName " +
                        "ORDER BY oi.inventoryItem.itemName ASC")
        List<RevenueReportItemAggregateDTO> getOrderAnalyticsItemsByItemNameAll(
                        @Param("itemName") String itemName);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.RevenueReportItemAggregateDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE UPPER(o.status.statusName) IN :statuses " +
                        "AND LOWER(oi.inventoryItem.itemName) LIKE LOWER(CONCAT('%', :itemName, '%')) " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName " +
                        "ORDER BY oi.inventoryItem.itemName ASC")
        List<RevenueReportItemAggregateDTO> getOrderAnalyticsItemsByItemNameAndStatusAll(
                        @Param("itemName") String itemName,
                        @Param("statuses") List<String> statuses);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.OrderAnalyticsSaleRowDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "UPPER(o.status.statusName), " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "o.createdTimestamp) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE oi.inventoryItem.itemId IN :itemIds " +
                        "GROUP BY oi.inventoryItem.itemId, o.status.statusName, o.createdTimestamp " +
                        "ORDER BY o.createdTimestamp ASC")
        List<OrderAnalyticsSaleRowDTO> getOrderAnalyticsSalesAll(
                        @Param("itemIds") List<UUID> itemIds);

        @Query("SELECT new com.example.ordermgmt.dto.analytics.OrderAnalyticsSaleRowDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "UPPER(o.status.statusName), " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "o.createdTimestamp) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE UPPER(o.status.statusName) IN :statuses " +
                        "AND oi.inventoryItem.itemId IN :itemIds " +
                        "GROUP BY oi.inventoryItem.itemId, o.status.statusName, o.createdTimestamp " +
                        "ORDER BY o.createdTimestamp ASC")
        List<OrderAnalyticsSaleRowDTO> getOrderAnalyticsSalesByStatusAll(
                        @Param("statuses") List<String> statuses,
                        @Param("itemIds") List<UUID> itemIds);
}
