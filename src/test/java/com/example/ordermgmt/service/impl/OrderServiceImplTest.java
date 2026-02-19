package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderItemDTO;
import com.example.ordermgmt.entity.*;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.InsufficientStockException;
import com.example.ordermgmt.exception.InvalidOrderTransitionException;
import com.example.ordermgmt.repository.OrdersRepository;
import com.example.ordermgmt.service.impl.order.OrderInventoryManagerImpl;
import com.example.ordermgmt.service.impl.order.OrderMapperImpl;
import com.example.ordermgmt.service.impl.order.OrderServiceImpl;
import com.example.ordermgmt.service.impl.order.OrderValidatorImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrdersRepository ordersRepository;
    @Mock
    private OrderValidatorImpl orderValidator;
    @Mock
    private OrderInventoryManagerImpl orderInventoryManager;
    @Mock
    private OrderMapperImpl orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("Create Order: Success")
    void createOrder_Success() {
        // Arrange
        String email = "customer@example.com";
        String itemId = "ITEM001";

        Customer customer = new Customer();
        customer.setCustomerId("CUST-1");

        OrderStatusLookup pendingStatus = new OrderStatusLookup();
        pendingStatus.setStatusName("PENDING");

        OrderDTO request = new OrderDTO();
        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setItemId(itemId);
        itemDTO.setQuantity(2);
        request.setItems(List.of(itemDTO));

        OrderDTO responseDTO = new OrderDTO();
        responseDTO.setStatus("PENDING");

        // Mock behaviors of delegates
        when(orderValidator.validateAndGetCustomer(email)).thenReturn(customer);
        when(orderValidator.getStatusOrThrow("PENDING")).thenReturn(pendingStatus);
        when(orderInventoryManager.processAndSaveOrderItems(eq(request.getItems()), any(Orders.class)))
                .thenReturn(List.of(itemDTO));
        when(orderMapper.calculateTotal(anyList())).thenReturn(new BigDecimal("200.00"));
        when(orderMapper.convertToDTO(any(Orders.class), anyList(), any(BigDecimal.class))).thenReturn(responseDTO);

        // Act
        OrderDTO result = orderService.createOrder(request, email);

        // Assert
        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        verify(ordersRepository, times(1)).save(any(Orders.class));
        verify(orderValidator).validateCustomerProfile(customer);
    }

    // Checking 'InsufficientStock' logic typically resides in OrderInventoryManager
    // now.
    // If OrderServiceImpl calls manager.process..., the manager throws exception.
    // So we should mock the manager to throw exception.
    @Test
    @DisplayName("Create Order: Fails Fast when Stock is Insufficient")
    void createOrder_InsufficientStock_ThrowsException() {
        // Arrange
        String email = "customer@example.com";
        String itemId = "ITEM001";

        Customer customer = new Customer();

        OrderDTO request = new OrderDTO();
        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setItemId(itemId);
        request.setItems(List.of(itemDTO));

        when(orderValidator.validateAndGetCustomer(email)).thenReturn(customer);
        when(orderValidator.getStatusOrThrow("PENDING")).thenReturn(new OrderStatusLookup());

        // Simulate Manager throwing exception
        doThrow(new InsufficientStockException("Insufficient stock")).when(orderInventoryManager)
                .processAndSaveOrderItems(eq(request.getItems()), any(Orders.class));

        // Act & Assert
        Exception exception = assertThrows(InsufficientStockException.class, () -> {
            orderService.createOrder(request, email);
        });

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(ordersRepository, times(1)).save(any(Orders.class)); // Order IS saved before items procesed in impl
    }

    @Test
    @DisplayName("Cancel Order: Fails if already CONFIRMED")
    void cancelOrder_Failure_AlreadyConfirmed() {
        // Arrange
        String orderId = "ORD-123";
        String email = "customer@example.com";

        Orders order = new Orders();
        order.setOrderId(orderId);

        when(ordersRepository.findById(orderId)).thenReturn(java.util.Optional.of(order));

        // Validator throws exception
        doThrow(new InvalidOrderTransitionException("Can only cancel PENDING orders"))
                .when(orderValidator).validateOrderCancellation(order);

        // Act & Assert
        Exception exception = assertThrows(InvalidOrderTransitionException.class, () -> {
            orderService.cancelOrder(orderId, email);
        });

        assertEquals("Can only cancel PENDING orders", exception.getMessage());
        verify(ordersRepository, never()).save(any(Orders.class)); // verification matches logic path
    }
}
