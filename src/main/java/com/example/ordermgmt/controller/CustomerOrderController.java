package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
@Tag(name = "2. My Orders", description = "Everything you need to place new orders, view your history, and manage existing purchases")
public class CustomerOrderController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerOrderController.class);
    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a New Order", description = "Create a new shopping order by providing the items and shipping information")
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO request, Authentication authentication) {
        String email = authentication.getName();
        logger.info("Customer order creation request: {}", email);
        return ResponseEntity.ok(orderService.createOrder(request, email));
    }

    @GetMapping
    @Operation(summary = "View My Order History", description = "Get a complete record of all the orders you have placed in the past")
    public ResponseEntity<List<OrderDTO>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        logger.info("Customer order history request: {}", email);
        return ResponseEntity.ok(orderService.getCustomerOrders(email));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get Order Details", description = "See the items, status, and total cost for a specific order by its ID")
    public ResponseEntity<OrderDTO> getMyOrderById(@PathVariable String orderId, Authentication authentication) {
        String email = authentication.getName();
        logger.info("Customer order detail request: {} for order: {}", email, orderId);
        return ResponseEntity.ok(orderService.getCustomerOrderById(orderId, email));
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an Order", description = "Stop a pending order from being processed if you have changed your mind")
    public ResponseEntity<OrderDTO> cancelMyOrder(@PathVariable String orderId, Authentication authentication) {
        String email = authentication.getName();
        logger.info("Customer order cancel request: {} for order: {}", email, orderId);
        return ResponseEntity.ok(orderService.cancelOrder(orderId, email));
    }
}
