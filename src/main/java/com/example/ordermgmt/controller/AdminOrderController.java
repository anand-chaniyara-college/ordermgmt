package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.BulkOrderUpdateResultDTO;
import com.example.ordermgmt.dto.BulkOrderStatusUpdateWrapperDTO;
import java.util.Map;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

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
import com.example.ordermgmt.dto.BulkOrderStatusUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "4. Order Management (Admin)", description = "Tools for administrators to oversee and manage all customer orders")
public class AdminOrderController {

    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);
    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "View Orders", description = "Get orders. With orderId: returns {\"orders\": [order]}. With page+size: returns paginated Page<OrderDTO>. Otherwise: returns {\"orders\": [...]}. customerId is excluded from responses.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<?> getAllOrders(
            @Parameter(description = "Specific Order ID to retrieve") @RequestParam(required = false) String orderId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size") @RequestParam(required = false) Integer size) {

        if (orderId != null && !orderId.isEmpty()) {
            logger.info("Processing getAllOrders for specific Order: {}", orderId);
            OrderDTO order = orderService.getOrderById(orderId);
            logger.info("getAllOrders completed successfully for Order: {}", orderId);
            return ResponseEntity.ok(Map.of("orders", List.of(order)));
        }

        if (page != null && size != null) {
            logger.info("Processing getAllOrders (Page) for Admin - Page: {}, Size: {}", page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<OrderDTO> orders = orderService.getAllOrders(pageable);
            logger.info("getAllOrders (Page) completed successfully for Admin");
            return ResponseEntity.ok(orders);
        }

        logger.info("Processing getAllOrders for Admin");
        List<OrderDTO> orders = orderService.getAllOrders();
        logger.info("getAllOrders completed successfully for Admin");
        return ResponseEntity.ok(Map.of("orders", orders));
    }

    @PutMapping("/status")
    @Operation(summary = "Bulk Update Order Status", description = "Update the status of multiple orders at once. Request body: {\"orders\": [{\"orderId\":\"...\",\"newStatus\":\"...\"},...]}. Each order is processed independently — one failure won't affect others.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk update result", content = @Content(schema = @Schema(implementation = BulkOrderUpdateResultDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request format", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<BulkOrderUpdateResultDTO> updateOrderStatusBulk(
            @Valid @RequestBody BulkOrderStatusUpdateWrapperDTO wrapper) {
        List<BulkOrderStatusUpdateDTO> updates = wrapper.getOrders();
        logger.info("Processing updateOrderStatusBulk for {} orders", updates.size());
        BulkOrderUpdateResultDTO result = orderService.updateOrdersStatus(updates);
        logger.info("updateOrderStatusBulk completed: {} successes, {} failures",
                result.getSuccesses().size(), result.getFailures().size());
        return ResponseEntity.ok(result);
    }
}
