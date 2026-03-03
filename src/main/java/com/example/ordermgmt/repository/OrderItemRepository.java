package com.example.ordermgmt.repository;

import com.example.ordermgmt.dto.analytics.ItemSalesReportDTO;
import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.entity.OrderItem;
import java.util.List;
import java.util.UUID;
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
}
