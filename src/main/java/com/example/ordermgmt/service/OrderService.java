package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderStatusUpdateDTO;

import java.util.List;

public interface OrderService {
    // Customer Operations
    OrderDTO createOrder(OrderDTO request, String email);

    List<OrderDTO> getCustomerOrders(String email);

    OrderDTO getCustomerOrderById(String orderId, String email);

    OrderDTO cancelOrder(String orderId, String email);

    // Admin Operations
    List<OrderDTO> getAllOrders();

    OrderDTO getOrderById(String orderId);

    OrderDTO updateOrderStatus(String orderId, OrderStatusUpdateDTO statusUpdate);
}
