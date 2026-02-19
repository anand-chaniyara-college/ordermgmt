package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderItemDTO;
import com.example.ordermgmt.entity.*;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.InsufficientStockException;
import com.example.ordermgmt.exception.InvalidOrderTransitionException;
import com.example.ordermgmt.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.ordermgmt.service.impl.order.OrderServiceImpl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrdersRepository ordersRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private InventoryItemRepository inventoryRepository;
    @Mock
    private OrderStatusLookupRepository statusRepository;

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
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setAddress("123 Street");

        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setItemId(itemId);
        inventoryItem.setItemName("Test Widget");
        inventoryItem.setAvailableStock(10);

        PricingCatalog pricing = new PricingCatalog();
        pricing.setItemId(itemId);
        pricing.setUnitPrice(new BigDecimal("100.00"));
        inventoryItem.setPricingCatalog(pricing);

        OrderStatusLookup pendingStatus = new OrderStatusLookup();
        pendingStatus.setStatusName("PENDING");

        OrderDTO request = new OrderDTO();
        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setItemId(itemId);
        itemDTO.setQuantity(2);
        request.setItems(List.of(itemDTO));

        when(customerRepository.findByAppUserEmail(email)).thenReturn(Optional.of(customer));
        when(statusRepository.findByStatusName("PENDING")).thenReturn(Optional.of(pendingStatus));
        when(inventoryRepository.findById(itemId)).thenReturn(Optional.of(inventoryItem));
        when(ordersRepository.save(any(Orders.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        OrderDTO result = orderService.createOrder(request, email);

        // Assert
        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        verify(ordersRepository, times(1)).save(any(Orders.class));
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("Create Order: Fails Fast when Stock is Insufficient")
    void createOrder_InsufficientStock_ThrowsException() {
        // Arrange
        String email = "customer@example.com";
        String itemId = "ITEM001";

        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setAddress("123 Street");

        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setItemId(itemId);
        inventoryItem.setItemName("Test Widget");
        inventoryItem.setAvailableStock(1); // Only 1 available

        PricingCatalog pricing = new PricingCatalog();
        pricing.setItemId(itemId);
        pricing.setUnitPrice(new BigDecimal("100.00"));
        inventoryItem.setPricingCatalog(pricing);

        OrderStatusLookup pendingStatus = new OrderStatusLookup();
        pendingStatus.setStatusName("PENDING");

        OrderDTO request = new OrderDTO();
        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setItemId(itemId);
        itemDTO.setQuantity(2); // Requesting 2
        request.setItems(List.of(itemDTO));

        when(customerRepository.findByAppUserEmail(email)).thenReturn(Optional.of(customer));
        when(statusRepository.findByStatusName("PENDING")).thenReturn(Optional.of(pendingStatus));
        when(inventoryRepository.findById(itemId)).thenReturn(Optional.of(inventoryItem));
        when(ordersRepository.save(any(Orders.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act & Assert
        Exception exception = assertThrows(InsufficientStockException.class, () -> {
            orderService.createOrder(request, email);
        });

        assertTrue(exception.getMessage().contains("Insufficient stock"));

        verify(ordersRepository, times(1)).save(any(Orders.class));
        // Verify we did NOT save the OrderItem
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("Cancel Order: Fails if already CONFIRMED")
    void cancelOrder_Failure_AlreadyConfirmed() {
        // Arrange
        String orderId = "ORD-123";
        String email = "customer@example.com";

        Customer customer = new Customer();
        AppUser user = new AppUser();
        user.setEmail(email);
        customer.setAppUser(user);

        OrderStatusLookup confirmedStatus = new OrderStatusLookup();
        confirmedStatus.setStatusName("CONFIRMED");

        Orders order = new Orders();
        order.setOrderId(orderId);
        order.setCustomer(customer);
        order.setStatus(confirmedStatus);

        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        Exception exception = assertThrows(InvalidOrderTransitionException.class, () -> {
            orderService.cancelOrder(orderId, email);
        });

        assertEquals("Can only cancel PENDING orders", exception.getMessage());
        verify(ordersRepository, never()).save(any(Orders.class));
    }
}
