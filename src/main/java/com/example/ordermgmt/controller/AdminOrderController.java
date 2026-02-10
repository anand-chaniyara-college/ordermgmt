package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderStatusUpdateDTO;
import com.example.ordermgmt.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        logger.info("Admin request: view all orders");
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable String orderId) {
        logger.info("Admin request: view order {}", orderId);
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable String orderId,
            @RequestBody OrderStatusUpdateDTO statusUpdate) {
        logger.info("Admin request: update status of order {} to {}", orderId, statusUpdate.getNewStatus());
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, statusUpdate));
    }
}
