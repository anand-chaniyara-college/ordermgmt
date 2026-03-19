package com.example.ordermgmt.scheduler;

import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.OrderNotFoundException;
import com.example.ordermgmt.repository.OrdersRepository;
import com.example.ordermgmt.service.impl.order.OrderTransitionHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background scheduler that auto-cancels PENDING orders older than 5 minutes.
 * Each cancellation runs in its own transaction via OrderTransitionHelper.
 */
@Component
@RequiredArgsConstructor
public class OrderAutoCancelScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OrderAutoCancelScheduler.class);
    private static final int STALE_MINUTES = 60;
    private static final String PENDING_STATUS = "PENDING";

    private final OrdersRepository ordersRepository;
    private final OrderTransitionHelper transitionHelper;

    @Scheduled(fixedRateString = "${SCHEDULER_FIXED_RATE_MS:300000}")
    public void cancelStalePendingOrders() {
        logger.info("Processing cancelStalePendingOrders for Scheduler");

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(STALE_MINUTES);
        List<Orders> staleOrders = ordersRepository.findStalePendingOrdersPriority(OrderStatus.PENDING.name(), cutoff);

        if (staleOrders.isEmpty()) {
            logger.info("cancelStalePendingOrders completed successfully for Scheduler: no stale orders found");
            return;
        }

        logger.info("Found {} stale PENDING orders (older than {} min). Auto-cancelling...",
                staleOrders.size(), STALE_MINUTES);

        for (Orders order : staleOrders) {
            try {
                transitionHelper.cancelStalePendingOrder(order.getOrderId());
            } catch (OrderNotFoundException e) {
                logger.error("cancelStalePendingOrder failed for Order: {}: {}", order.getOrderId(), e.getMessage());
            }
        }

        logger.info("cancelStalePendingOrders completed successfully for Scheduler: processed {} orders",
                staleOrders.size());
    }
}
