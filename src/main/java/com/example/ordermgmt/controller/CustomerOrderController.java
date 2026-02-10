package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerOrderController.class);
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO request, Authentication authentication) {
        String email = authentication.getName();
        logger.info("Customer order creation request: {}", email);
        return ResponseEntity.ok(orderService.createOrder(request, email));
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        logger.info("Customer order history request: {}", email);
        return ResponseEntity.ok(orderService.getCustomerOrders(email));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getMyOrderById(@PathVariable String orderId, Authentication authentication) {
        String email = authentication.getName();
        logger.info("Customer order detail request: {} for order: {}", email, orderId);
        return ResponseEntity.ok(orderService.getCustomerOrderById(orderId, email));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDTO> cancelMyOrder(@PathVariable String orderId, Authentication authentication) {
        String email = authentication.getName();
        logger.info("Customer order cancel request: {} for order: {}", email, orderId);
        return ResponseEntity.ok(orderService.cancelOrder(orderId, email));
    }
}
