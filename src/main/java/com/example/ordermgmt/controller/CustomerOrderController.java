package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody OrderDTO request, Authentication authentication) {
        String email = authentication.getName();
        logger.info("Processing createOrder for Customer: {}", email);
        OrderDTO order = orderService.createOrder(request, email);
        logger.info("createOrder completed successfully for Customer: {}", email);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(order);
    }

    @GetMapping
    @Operation(summary = "View My Order History", description = "Get a complete record of all the orders you have placed in the past")
    public ResponseEntity<List<OrderDTO>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        logger.info("Processing getMyOrders for Customer: {}", email);
        List<OrderDTO> orders = orderService.getCustomerOrders(email);
        logger.info("getMyOrders completed successfully for Customer: {}", email);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get Order Details", description = "See the items, status, and total cost for a specific order by its ID")
    public ResponseEntity<OrderDTO> getMyOrderById(@PathVariable String orderId, Authentication authentication) {
        String email = authentication.getName();
        logger.info("Processing getMyOrderById for Order: {}", orderId);
        OrderDTO order = orderService.getCustomerOrderById(orderId, email);
        logger.info("getMyOrderById completed successfully for Order: {}", orderId);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an Order", description = "Stop a pending order from being processed if you have changed your mind")
    public ResponseEntity<OrderDTO> cancelMyOrder(@PathVariable String orderId, Authentication authentication) {
        String email = authentication.getName();
        logger.info("Processing cancelMyOrder for Order: {}", orderId);
        OrderDTO order = orderService.cancelOrder(orderId, email);
        logger.info("cancelMyOrder completed successfully for Order: {}", orderId);
        return ResponseEntity.ok(order);
    }
}
