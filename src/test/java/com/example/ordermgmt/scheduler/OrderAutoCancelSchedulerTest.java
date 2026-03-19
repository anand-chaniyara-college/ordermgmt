package com.example.ordermgmt.scheduler;

import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.exception.OrderNotFoundException;
import com.example.ordermgmt.repository.OrdersRepository;
import com.example.ordermgmt.service.impl.order.OrderTransitionHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAutoCancelSchedulerTest {

    @Mock
    private OrdersRepository ordersRepository;

    @Mock
    private OrderTransitionHelper transitionHelper;

    @InjectMocks
    private OrderAutoCancelScheduler scheduler;

    @Test
    void cancelStalePendingOrders_UsesUnlockedCandidateQueryAndCancelsEachOrder() {
        Orders first = new Orders();
        first.setOrderId(UUID.randomUUID());

        Orders second = new Orders();
        second.setOrderId(UUID.randomUUID());

        when(ordersRepository.findStalePendingOrders(eq("PENDING"), any()))
                .thenReturn(List.of(first, second));

        scheduler.cancelStalePendingOrders();

        verify(ordersRepository).findStalePendingOrders(eq("PENDING"), any());
        verify(transitionHelper).cancelStalePendingOrder(first.getOrderId());
        verify(transitionHelper).cancelStalePendingOrder(second.getOrderId());
        verifyNoMoreInteractions(ordersRepository);
    }

    @Test
    void cancelStalePendingOrders_ContinuesWhenAnOrderDisappearsMidLoop() {
        Orders first = new Orders();
        first.setOrderId(UUID.randomUUID());

        Orders second = new Orders();
        second.setOrderId(UUID.randomUUID());

        when(ordersRepository.findStalePendingOrders(eq("PENDING"), any()))
                .thenReturn(List.of(first, second));
        doThrow(new OrderNotFoundException("missing"))
                .when(transitionHelper).cancelStalePendingOrder(first.getOrderId());

        scheduler.cancelStalePendingOrders();

        verify(transitionHelper).cancelStalePendingOrder(first.getOrderId());
        verify(transitionHelper).cancelStalePendingOrder(second.getOrderId());
        verify(ordersRepository, times(1)).findStalePendingOrders(eq("PENDING"), any());
    }
}
