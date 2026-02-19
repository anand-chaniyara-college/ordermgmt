package com.example.ordermgmt.service.impl.order;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.entity.OrderStatusLookup;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.OrderNotFoundException;
import com.example.ordermgmt.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles individual order status transitions in an isolated transaction.
 * Uses REQUIRES_NEW propagation so that:
 * - In bulk updates, one failure doesn't roll back other orders.
 * - The auto-cancel scheduler can cancel each order independently.
 */
@Component
@RequiredArgsConstructor
public class OrderTransitionHelper {

    private static final Logger logger = LoggerFactory.getLogger(OrderTransitionHelper.class);

    private final OrdersRepository ordersRepository;
    private final OrderValidatorImpl orderValidator;
    private final OrderInventoryManagerImpl orderInventoryManager;
    private final OrderMapperImpl orderMapper;

    /**
     * Perform a status transition for a single order in its own transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderDTO updateOrderInternal(String orderId, String newStatusString) {
        logger.info("Processing updateOrderInternal for Order: {}", orderId);

        String newStatusName = newStatusString.trim().toUpperCase();

        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.warn("Skipping updateOrderInternal for Order: {} - Order not found", orderId);
                    return new OrderNotFoundException("Order not found: " + orderId);
                });

        OrderStatus currentStatus = OrderStatus.valueOf(order.getStatus().getStatusName());
        OrderStatus nextStatus = OrderStatus.valueOf(newStatusName);

        orderValidator.validateAdminTransition(currentStatus, nextStatus);

        OrderStatusLookup nextStatusLookup = orderValidator.getStatusOrThrow(newStatusName);
        orderInventoryManager.handleInventoryUpdate(order, currentStatus, nextStatus);

        order.setStatus(nextStatusLookup);
        ordersRepository.save(order);

        logger.info("updateOrderInternal completed successfully for Order: {} ({} -> {})",
                orderId, currentStatus, nextStatus);
        return orderMapper.convertToDTO(order);
    }

    /**
     * Cancel a stale PENDING order. Used by the auto-cancel scheduler.
     * Double-checks that the order is still PENDING before cancelling.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelStalePendingOrder(String orderId) {
        logger.info("Processing cancelStalePendingOrder for Order: {}", orderId);

        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.warn("Skipping cancelStalePendingOrder for Order: {} - Order not found", orderId);
                    return new OrderNotFoundException("Order not found: " + orderId);
                });

        if (!OrderStatus.PENDING.name().equals(order.getStatus().getStatusName())) {
            logger.warn("Skipping cancelStalePendingOrder for Order: {} - no longer PENDING (current: {})",
                    orderId, order.getStatus().getStatusName());
            return;
        }

        OrderStatusLookup cancelledStatus = orderValidator.getStatusOrThrow(OrderStatus.CANCELLED.name());
        order.setStatus(cancelledStatus);
        ordersRepository.save(order);

        logger.info("cancelStalePendingOrder completed successfully for Order: {}", orderId);
    }
}
