package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderStatusUpdateDTO;
import com.example.ordermgmt.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "4. Order Management (Admin)", description = "Tools for administrators to oversee and manage all customer orders")
public class AdminOrderController {

    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);
    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "View All Orders", description = "Get a list of every order placed in the system")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        logger.info("Processing getAllOrders for Admin");
        List<OrderDTO> orders = orderService.getAllOrders();
        logger.info("getAllOrders completed successfully for Admin");
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "View Detailed Order", description = "Get full details of a specific order using its unique ID")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable String orderId) {
        logger.info("Processing getOrderById for Order: {}", orderId);
        OrderDTO order = orderService.getOrderById(orderId);
        logger.info("getOrderById completed successfully for Order: {}", orderId);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update Order Status", description = "Change the progress of an order (e.g., mark as Shipped or Delivered)")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable String orderId,
            @Valid @RequestBody OrderStatusUpdateDTO statusUpdate) {
        logger.info("Processing updateOrderStatus for Order: {}", orderId);
        OrderDTO updatedOrder = orderService.updateOrderStatus(orderId, statusUpdate);
        logger.info("updateOrderStatus completed successfully for Order: {}", orderId);
        return ResponseEntity.ok(updatedOrder);
    }
}
