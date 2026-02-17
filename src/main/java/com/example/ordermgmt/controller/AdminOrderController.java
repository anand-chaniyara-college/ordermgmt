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
        logger.info("Admin request: view all orders");
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "View Detailed Order", description = "Get full details of a specific order using its unique ID")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable String orderId) {
        logger.info("Admin request: view order {}", orderId);
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update Order Status", description = "Change the progress of an order (e.g., mark as Shipped or Delivered)")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable String orderId,
            @Valid @RequestBody OrderStatusUpdateDTO statusUpdate) {
        logger.info("Admin request: update status of order {} to {}", orderId, statusUpdate.getNewStatus());
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, statusUpdate));
    }
}
