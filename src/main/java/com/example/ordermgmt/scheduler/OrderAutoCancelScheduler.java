package com.example.ordermgmt.scheduler;

import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.OrderNotFoundException;
import com.example.ordermgmt.repository.OrdersRepository;
import com.example.ordermgmt.service.impl.order.OrderTransitionHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderAutoCancelScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OrderAutoCancelScheduler.class);

    @Value("${app.order.stale-minutes}")
    private int staleMinutes;

    private static final String PENDING_STATUS = "PENDING";

    private final OrdersRepository ordersRepository;
    private final OrderTransitionHelper transitionHelper;

    @Scheduled(fixedRateString = "${app.scheduler.fixed-rate-ms}")
    public void cancelStalePendingOrders() {
        logger.info("Processing cancelStalePendingOrders for Scheduler");


        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(staleMinutes);
        List<Orders> staleOrders = ordersRepository.findStalePendingOrders(OrderStatus.PENDING.name(), cutoff);

        if (staleOrders.isEmpty()) {
            logger.info("cancelStalePendingOrders completed successfully for Scheduler: no stale orders found");
            return;
        }

        logger.info("Found {} stale PENDING orders (older than {} min). Auto-cancelling...",
                staleOrders.size(), staleMinutes);

        for (Orders order : staleOrders) {
            try {
                transitionHelper.cancelStalePendingOrder(order.getOrderId());
            } catch (OrderNotFoundException e) {
                logger.error("cancelStalePendingOrder failed for Order: {}", order.getOrderId(), e);
            }
        }

        logger.info("cancelStalePendingOrders completed successfully for Scheduler: processed {} orders",
                staleOrders.size());
    }
}
