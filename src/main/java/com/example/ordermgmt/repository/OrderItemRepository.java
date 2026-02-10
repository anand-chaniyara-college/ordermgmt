package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItem.OrderItemId> {
    List<OrderItem> findByOrderOrderId(String orderId);
}
