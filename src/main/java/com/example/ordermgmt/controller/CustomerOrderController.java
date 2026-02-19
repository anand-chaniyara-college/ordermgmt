package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import com.example.ordermgmt.dto.BulkOrderStatusUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

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
    @Operation(summary = "View My Orders (Merged)", description = "Get your orders. Returns specific order if orderId provided, paginated result if page/size provided, or full list otherwise. Always returns a list format.")
    public ResponseEntity<?> getMyOrders(
            Authentication authentication,
            @Parameter(description = "Specific Order ID to retrieve") @RequestParam(required = false) String orderId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size") @RequestParam(required = false) Integer size) {

        String email = authentication.getName();

        if (orderId != null && !orderId.isEmpty()) {
            logger.info("Processing getMyOrders for specific Order: {}, Customer: {}", orderId, email);
            OrderDTO order = orderService.getCustomerOrderById(orderId, email);
            logger.info("getMyOrders completed successfully for Order: {}", orderId);
            // Returning as a List containing the single object as strictly requested
            return ResponseEntity.ok(List.of(order));
        }

        if (page != null && size != null) {
            logger.info("Processing getMyOrders (Page) for Customer: {} - Page: {}, Size: {}", email, page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<OrderDTO> orders = orderService.getCustomerOrders(email, pageable);
            logger.info("getMyOrders (Page) completed successfully for Customer: {}", email);
            return ResponseEntity.ok(orders);
        }

        logger.info("Processing getMyOrders for Customer: {}", email);
        List<OrderDTO> orders = orderService.getCustomerOrders(email);
        logger.info("getMyOrders completed successfully for Customer: {}", email);
        return ResponseEntity.ok(orders);
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
