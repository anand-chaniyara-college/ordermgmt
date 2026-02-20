package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.OrderDTO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

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
import java.util.Map;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
@Tag(name = "2. My Orders", description = "Everything you need to place new orders, view your history, and manage existing purchases")
public class CustomerOrderController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerOrderController.class);
    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a New Order", description = "Create a new shopping order by providing the items. Response includes orderId, status, timestamps, items with prices, and totalAmount. customerId is excluded from response.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully", content = @Content(schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request — missing items, invalid quantity, or insufficient stock", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody OrderDTO request, Authentication authentication) {
        String email = authentication.getName();
        logger.info("Processing createOrder for Customer: {}", email);
        OrderDTO order = orderService.createOrder(request, email);
        logger.info("createOrder completed successfully for Customer: {}", email);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(order);
    }

    @GetMapping
    @Operation(summary = "View My Orders", description = "Get your orders. With orderId: returns {\"orders\": [order]}. With page+size: returns paginated Page<OrderDTO>. Otherwise: returns {\"orders\": [...]}. customerId is excluded from all responses.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
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
            return ResponseEntity.ok(Map.of("orders", List.of(order)));
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
        return ResponseEntity.ok(Map.of("orders", orders));
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an Order", description = "Cancel a PENDING order. Only the order owner can cancel. Returns updated OrderDTO with CANCELLED status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully", content = @Content(schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "400", description = "Order cannot be cancelled (not in PENDING status)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<OrderDTO> cancelMyOrder(@PathVariable String orderId, Authentication authentication) {
        String email = authentication.getName();
        logger.info("Processing cancelMyOrder for Order: {}", orderId);
        OrderDTO order = orderService.cancelOrder(orderId, email);
        logger.info("cancelMyOrder completed successfully for Order: {}", orderId);
        return ResponseEntity.ok(order);
    }
}
