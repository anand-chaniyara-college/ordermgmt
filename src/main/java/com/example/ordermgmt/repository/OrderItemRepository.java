package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItem.OrderItemId> {
        List<OrderItem> findByOrderOrderId(String orderId);

        @org.springframework.data.jpa.repository.Query("SELECT new com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO("
                        +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE FUNCTION('date_part', 'year', o.createdTimestamp) = :year " +
                        "AND FUNCTION('date_part', 'month', o.createdTimestamp) = :month")
        com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO getMonthlyReport(
                        @org.springframework.data.repository.query.Param("month") int month,
                        @org.springframework.data.repository.query.Param("year") int year);

        @org.springframework.data.jpa.repository.Query("SELECT new com.example.ordermgmt.dto.analytics.ItemSalesReportDTO("
                        +
                        "oi.inventoryItem.itemId, " +
                        "oi.inventoryItem.itemName, " +
                        "CAST(COALESCE(SUM(oi.quantity), 0) AS Long), " +
                        "CAST(COALESCE(SUM(oi.quantity * oi.unitPrice), 0) AS BigDecimal)) " +
                        "FROM OrderItem oi " +
                        "JOIN oi.order o " +
                        "WHERE FUNCTION('date_part', 'year', o.createdTimestamp) = :year " +
                        "AND FUNCTION('date_part', 'month', o.createdTimestamp) = :month " +
                        "GROUP BY oi.inventoryItem.itemId, oi.inventoryItem.itemName")
        java.util.List<com.example.ordermgmt.dto.analytics.ItemSalesReportDTO> getMonthlyItemWiseReport(
                        @org.springframework.data.repository.query.Param("month") int month,
                        @org.springframework.data.repository.query.Param("year") int year);
}
