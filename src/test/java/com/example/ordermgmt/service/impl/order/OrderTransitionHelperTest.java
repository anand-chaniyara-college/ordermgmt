package com.example.ordermgmt.service.impl.order;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.entity.OrderStatusLookup;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.event.EmailDispatchEvent;
import com.example.ordermgmt.exception.InvalidOrderTransitionException;
import com.example.ordermgmt.exception.OrderNotFoundException;
import com.example.ordermgmt.repository.OrdersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderTransitionHelperTest {

    @Mock
    private OrdersRepository ordersRepository;

    @Mock
    private OrderValidatorImpl orderValidator;

    @Mock
    private OrderInventoryManagerImpl orderInventoryManager;

    @Mock
    private OrderMapperImpl orderMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderTransitionHelper orderTransitionHelper;

    private UUID orderId;
    private UUID customerId;
    private UUID orgId;
    private Orders order;
    private Customer customer;
    private AppUser appUser;
    private OrderStatusLookup pendingStatus;
    private OrderStatusLookup confirmedStatus;
    private OrderStatusLookup cancelledStatus;
    private OrderDTO orderDTO;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        appUser = new AppUser();
        appUser.setEmail("customer@example.com");

        customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setAppUser(appUser);
        customer.setOrgId(orgId);

        pendingStatus = new OrderStatusLookup();
        pendingStatus.setStatusName("PENDING");

        confirmedStatus = new OrderStatusLookup();
        confirmedStatus.setStatusName("CONFIRMED");

        cancelledStatus = new OrderStatusLookup();
        cancelledStatus.setStatusName("CANCELLED");

        order = new Orders();
        order.setOrderId(orderId);
        order.setCustomer(customer);
        order.setStatus(pendingStatus);
        order.setCreatedTimestamp(LocalDateTime.now());

        orderDTO = new OrderDTO(orderId, customerId, "PENDING", 
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    @Test
    void updateOrderInternal_WithValidTransition_UpdatesSuccessfully() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
        doNothing().when(orderValidator).validateAdminTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED);
        when(orderValidator.getStatusOrThrow("CONFIRMED")).thenReturn(confirmedStatus);
        doNothing().when(orderInventoryManager).handleInventoryUpdate(order, OrderStatus.PENDING, OrderStatus.CONFIRMED);
        when(orderMapper.convertToDTO(order)).thenReturn(orderDTO);

        OrderDTO result = orderTransitionHelper.updateOrderInternal(orderId, "CONFIRMED");

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(confirmedStatus, order.getStatus());
        verify(ordersRepository).save(order);
        verify(eventPublisher).publishEvent(any(EmailDispatchEvent.class));
    }

    @Test
    void updateOrderInternal_WithNonExistingOrder_ThrowsException() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () ->
                orderTransitionHelper.updateOrderInternal(orderId, "CONFIRMED"));
        
        verify(orderValidator, never()).validateAdminTransition(any(), any());
        verify(orderInventoryManager, never()).handleInventoryUpdate(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateOrderInternal_WithInvalidTransition_ThrowsException() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
        doThrow(new InvalidOrderTransitionException("Invalid transition"))
                .when(orderValidator).validateAdminTransition(OrderStatus.PENDING, OrderStatus.SHIPPED);

        assertThrows(InvalidOrderTransitionException.class, () ->
                orderTransitionHelper.updateOrderInternal(orderId, "SHIPPED"));
        
        verify(orderInventoryManager, never()).handleInventoryUpdate(any(), any(), any());
        verify(ordersRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateOrderInternal_TrimsAndUpperCasesStatus() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
        doNothing().when(orderValidator).validateAdminTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED);
        when(orderValidator.getStatusOrThrow("CONFIRMED")).thenReturn(confirmedStatus);
        doNothing().when(orderInventoryManager).handleInventoryUpdate(order, OrderStatus.PENDING, OrderStatus.CONFIRMED);
        when(orderMapper.convertToDTO(order)).thenReturn(orderDTO);

        orderTransitionHelper.updateOrderInternal(orderId, "  confirmed  ");

        verify(orderValidator).validateAdminTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED);
        verify(orderValidator).getStatusOrThrow("CONFIRMED");
    }

    @Test
    void updateOrderInternal_WithNullFirstName_UsesEmailForNotification() {
        customer.setFirstName(null);
        customer.setLastName(null);

        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
        doNothing().when(orderValidator).validateAdminTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED);
        when(orderValidator.getStatusOrThrow("CONFIRMED")).thenReturn(confirmedStatus);
        doNothing().when(orderInventoryManager).handleInventoryUpdate(order, OrderStatus.PENDING, OrderStatus.CONFIRMED);
        when(orderMapper.convertToDTO(order)).thenReturn(orderDTO);

        orderTransitionHelper.updateOrderInternal(orderId, "CONFIRMED");

        ArgumentCaptor<EmailDispatchEvent> eventCaptor = ArgumentCaptor.forClass(EmailDispatchEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        EmailDispatchEvent event = eventCaptor.getValue();
        assertEquals("customer@example.com", event.recipientEmail());
        assertEquals(orgId, event.orgId());
    }

    @Test
    void cancelStalePendingOrder_WithPendingOrder_CancelsSuccessfully() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderValidator.getStatusOrThrow("CANCELLED")).thenReturn(cancelledStatus);

        orderTransitionHelper.cancelStalePendingOrder(orderId);

        assertEquals(cancelledStatus, order.getStatus());
        verify(ordersRepository).save(order);
        verify(eventPublisher).publishEvent(any(EmailDispatchEvent.class));
    }

    @Test
    void cancelStalePendingOrder_WithNonPendingOrder_DoesNothing() {
        order.setStatus(confirmedStatus);
        
        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderTransitionHelper.cancelStalePendingOrder(orderId);

        assertEquals(confirmedStatus, order.getStatus()); // unchanged
        verify(ordersRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void cancelStalePendingOrder_WithNonExistingOrder_ThrowsException() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () ->
                orderTransitionHelper.cancelStalePendingOrder(orderId));
        
        verify(ordersRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void cancelStalePendingOrder_WithNullCustomerName_UsesEmail() {
        customer.setFirstName(null);
        customer.setLastName(null);

        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderValidator.getStatusOrThrow("CANCELLED")).thenReturn(cancelledStatus);

        orderTransitionHelper.cancelStalePendingOrder(orderId);

        ArgumentCaptor<EmailDispatchEvent> eventCaptor = ArgumentCaptor.forClass(EmailDispatchEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        EmailDispatchEvent event = eventCaptor.getValue();
        assertEquals("customer@example.com", event.recipientEmail());
    }

    @Test
    void cancelStalePendingOrder_WithOnlyFirstName_CombinesName() {
        customer.setFirstName("John");
        customer.setLastName(null);

        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderValidator.getStatusOrThrow("CANCELLED")).thenReturn(cancelledStatus);

        orderTransitionHelper.cancelStalePendingOrder(orderId);

        ArgumentCaptor<EmailDispatchEvent> eventCaptor = ArgumentCaptor.forClass(EmailDispatchEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        EmailDispatchEvent event = eventCaptor.getValue();
        assertEquals("customer@example.com", event.recipientEmail());
    }
}
