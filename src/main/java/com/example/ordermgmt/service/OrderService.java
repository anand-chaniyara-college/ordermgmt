package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.BulkOrderStatusUpdateDTO;
import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderStatusUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    // Customer Operations
    OrderDTO createOrder(OrderDTO request, String email);

    List<OrderDTO> getCustomerOrders(String email);

    Page<OrderDTO> getCustomerOrders(String email, Pageable pageable);

    OrderDTO getCustomerOrderById(String orderId, String email);

    OrderDTO cancelOrder(String orderId, String email);

    // Admin Operations
    List<OrderDTO> getAllOrders();

    Page<OrderDTO> getAllOrders(Pageable pageable);

    OrderDTO getOrderById(String orderId);

    OrderDTO updateOrderStatus(String orderId, OrderStatusUpdateDTO statusUpdate);

    List<OrderDTO> updateOrdersStatus(List<BulkOrderStatusUpdateDTO> updates);
}
