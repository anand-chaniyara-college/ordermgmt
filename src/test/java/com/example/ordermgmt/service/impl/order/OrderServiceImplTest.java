package com.example.ordermgmt.service.impl.order;

import com.example.ordermgmt.dto.*;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.entity.OrderStatusLookup;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.event.EmailDispatchEvent;
import com.example.ordermgmt.exception.InsufficientStockException;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.OrderNotFoundException;
import com.example.ordermgmt.repository.OrdersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Mock
    private OrderTransitionHelper transitionHelper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID orderId;
    private UUID customerId;
    private String email;
    private Customer customer;
    private Orders order;
    private OrderStatusLookup pendingStatus;
    private OrderDTO orderDTO;
    private OrderItemDTO orderItemDTO;
    private List<OrderItemDTO> itemDTOs;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        email = "customer@example.com";

        customer = new Customer();
        customer.setCustomerId(customerId);
        com.example.ordermgmt.entity.AppUser appUser = new com.example.ordermgmt.entity.AppUser();
        appUser.setEmail(email);
        customer.setAppUser(appUser);

        pendingStatus = new OrderStatusLookup();
        pendingStatus.setStatusName("PENDING");

        order = new Orders();
        order.setOrderId(orderId);
        order.setCustomer(customer);
        order.setStatus(pendingStatus);
        order.setCreatedTimestamp(LocalDateTime.now());

        orderItemDTO = new OrderItemDTO(UUID.randomUUID(), "Test Item", 2, 
                BigDecimal.valueOf(49.99), BigDecimal.valueOf(99.98));
        itemDTOs = List.of(orderItemDTO);

        orderDTO = new OrderDTO(orderId, customerId, "PENDING", 
                LocalDateTime.now(), LocalDateTime.now(), 
                itemDTOs, BigDecimal.valueOf(99.98));
    }

    @Test
    void createOrder_WithValidRequest_CreatesSuccessfully() {
        when(orderValidator.validateAndGetCustomer(email)).thenReturn(customer);
        doNothing().when(orderValidator).validateCustomerProfile(customer);
        when(orderValidator.getStatusOrThrow("PENDING")).thenReturn(pendingStatus);
        when(ordersRepository.saveAndFlush(any(Orders.class))).thenReturn(order);
        when(orderInventoryManager.processAndSaveOrderItems(eq(itemDTOs), any(Orders.class)))
                .thenReturn(itemDTOs);
        when(orderMapper.calculateTotal(itemDTOs)).thenReturn(BigDecimal.valueOf(99.98));
        when(orderMapper.convertToDTO(any(Orders.class), eq(itemDTOs), any(BigDecimal.class)))
                .thenReturn(orderDTO);

        OrderDTO result = orderService.createOrder(orderDTO, email);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(customerId, result.getCustomerId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals(BigDecimal.valueOf(99.98), result.getTotalAmount());

        verify(eventPublisher).publishEvent(any(EmailDispatchEvent.class));
    }

    @Test
    void createOrder_WithInvalidCustomer_ThrowsException() {
        when(orderValidator.validateAndGetCustomer(email))
                .thenThrow(new InvalidOperationException("Customer not found"));

        assertThrows(InvalidOperationException.class, () ->
                orderService.createOrder(orderDTO, email));
        
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void getCustomerOrders_AsList_ReturnsOrders() {
        when(ordersRepository.findByCustomerAppUserEmail(email)).thenReturn(List.of(order));
        when(orderMapper.convertToDTO(order)).thenReturn(orderDTO);

        List<OrderDTO> result = orderService.getCustomerOrders(email);

        assertEquals(1, result.size());
        assertEquals(orderId, result.get(0).getOrderId());
    }

    @Test
    void getCustomerOrders_AsList_WithNoOrders_ReturnsEmptyList() {
        when(ordersRepository.findByCustomerAppUserEmail(email)).thenReturn(List.of());

        List<OrderDTO> result = orderService.getCustomerOrders(email);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCustomerOrders_AsPage_ReturnsPagedOrders() {
        Page<Orders> orderPage = new PageImpl<>(List.of(order));
        when(ordersRepository.findByCustomerAppUserEmail(eq(email), any(Pageable.class)))
                .thenReturn(orderPage);
        when(orderMapper.convertToDTO(order)).thenReturn(orderDTO);

        Page<OrderDTO> result = orderService.getCustomerOrders(email, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals(orderId, result.getContent().get(0).getOrderId());
    }

    @Test
    void getCustomerOrderById_WithValidOwnership_ReturnsOrder() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
        doNothing().when(orderValidator).validateOrderOwnership(order, email);
        when(orderMapper.convertToDTO(order)).thenReturn(orderDTO);

        OrderDTO result = orderService.getCustomerOrderById(orderId, email);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
    }

    @Test
    void getCustomerOrderById_WithNonExistingOrder_ThrowsException() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () ->
                orderService.getCustomerOrderById(orderId, email));
    }

    @Test
    void getCustomerOrderById_WithWrongOwner_ThrowsException() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
        doThrow(new InvalidOperationException("Access denied"))
                .when(orderValidator).validateOrderOwnership(order, email);

        assertThrows(InvalidOperationException.class, () ->
                orderService.getCustomerOrderById(orderId, email));
    }

    @Test
    void cancelOrder_WithValidRequest_CancelsSuccessfully() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
        doNothing().when(orderValidator).validateOrderOwnership(order, email);
        doNothing().when(orderValidator).validateOrderCancellation(order);
        when(orderValidator.getStatusOrThrow("CANCELLED")).thenReturn(pendingStatus); // reuse pending status for test
        doNothing().when(orderInventoryManager).handleInventoryUpdate(eq(order), eq(OrderStatus.PENDING), eq(OrderStatus.CANCELLED));
        when(orderMapper.convertToDTO(order)).thenReturn(orderDTO);

        OrderDTO result = orderService.cancelOrder(orderId, email);

        assertNotNull(result);
        assertEquals(pendingStatus, order.getStatus());
        verify(ordersRepository).save(order);
        verify(eventPublisher).publishEvent(any(EmailDispatchEvent.class));
    }

    @Test
    void cancelOrder_WithInvalidOwnership_ThrowsException() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
        doThrow(new InvalidOperationException("Access denied"))
                .when(orderValidator).validateOrderOwnership(order, email);

        assertThrows(InvalidOperationException.class, () ->
                orderService.cancelOrder(orderId, email));
    }

    @Test
    void getAllOrders_AsList_ReturnsAllOrders() {
        when(ordersRepository.findAll()).thenReturn(List.of(order));
        when(orderMapper.convertToDTO(order)).thenReturn(orderDTO);

        List<OrderDTO> result = orderService.getAllOrders();

        assertEquals(1, result.size());
        assertEquals(orderId, result.get(0).getOrderId());
    }

    @Test
    void getAllOrders_AsPage_ReturnsPagedOrders() {
        Page<Orders> orderPage = new PageImpl<>(List.of(order));
        when(ordersRepository.findAll(any(Pageable.class))).thenReturn(orderPage);
        when(orderMapper.convertToDTO(order)).thenReturn(orderDTO);

        Page<OrderDTO> result = orderService.getAllOrders(PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getOrderById_WithExistingOrder_ReturnsOrder() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.convertToDTO(order)).thenReturn(orderDTO);

        OrderDTO result = orderService.getOrderById(orderId);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
    }

    @Test
    void getOrderById_WithNonExistingOrder_ThrowsException() {
        when(ordersRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () ->
                orderService.getOrderById(orderId));
    }

    @Test
    void updateOrderStatus_WithValidTransition_UpdatesSuccessfully() {
        OrderStatusUpdateDTO statusUpdate = new OrderStatusUpdateDTO("CONFIRMED");
        when(transitionHelper.updateOrderInternal(orderId, "CONFIRMED")).thenReturn(orderDTO);

        OrderDTO result = orderService.updateOrderStatus(orderId, statusUpdate);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
    }

    @Test
    void updateOrdersStatus_BulkUpdate_ReturnsSuccessAndFailures() {
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        UUID orderId3 = UUID.randomUUID();

        List<BulkOrderStatusUpdateDTO> updates = List.of(
                new BulkOrderStatusUpdateDTO(orderId1, "CONFIRMED"),
                new BulkOrderStatusUpdateDTO(orderId2, "INVALID"),
                new BulkOrderStatusUpdateDTO(orderId3, "CANCELLED")
        );

        OrderDTO successDTO1 = new OrderDTO(orderId1, customerId, "CONFIRMED", null, null, null, null);
        OrderDTO successDTO3 = new OrderDTO(orderId3, customerId, "CANCELLED", null, null, null, null);

        when(transitionHelper.updateOrderInternal(orderId1, "CONFIRMED")).thenReturn(successDTO1);
        when(transitionHelper.updateOrderInternal(orderId2, "INVALID"))
                .thenThrow(new InvalidOperationException("Invalid transition"));
        when(transitionHelper.updateOrderInternal(orderId3, "CANCELLED")).thenReturn(successDTO3);

        BulkOrderUpdateResultDTO result = orderService.updateOrdersStatus(updates);

        assertEquals(2, result.getSuccesses().size());
        assertEquals(1, result.getFailures().size());
        assertEquals(orderId2, result.getFailures().get(0).getOrderId());
        assertNotNull(result.getFailures().get(0).getError());
    }

    @Test
    void updateOrdersStatus_WithAllSuccesses_ReturnsAllSuccess() {
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();

        List<BulkOrderStatusUpdateDTO> updates = List.of(
                new BulkOrderStatusUpdateDTO(orderId1, "CONFIRMED"),
                new BulkOrderStatusUpdateDTO(orderId2, "SHIPPED")
        );

        OrderDTO successDTO1 = new OrderDTO(orderId1, customerId, "CONFIRMED", null, null, null, null);
        OrderDTO successDTO2 = new OrderDTO(orderId2, customerId, "SHIPPED", null, null, null, null);

        when(transitionHelper.updateOrderInternal(orderId1, "CONFIRMED")).thenReturn(successDTO1);
        when(transitionHelper.updateOrderInternal(orderId2, "SHIPPED")).thenReturn(successDTO2);

        BulkOrderUpdateResultDTO result = orderService.updateOrdersStatus(updates);

        assertEquals(2, result.getSuccesses().size());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    void updateOrdersStatus_WithAllFailures_ReturnsAllFailures() {
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();

        List<BulkOrderStatusUpdateDTO> updates = List.of(
                new BulkOrderStatusUpdateDTO(orderId1, "INVALID1"),
                new BulkOrderStatusUpdateDTO(orderId2, "INVALID2")
        );

        when(transitionHelper.updateOrderInternal(orderId1, "INVALID1"))
                .thenThrow(new InvalidOperationException("Invalid transition 1"));
        when(transitionHelper.updateOrderInternal(orderId2, "INVALID2"))
                .thenThrow(new InsufficientStockException("Insufficient stock"));

        BulkOrderUpdateResultDTO result = orderService.updateOrdersStatus(updates);

        assertTrue(result.getSuccesses().isEmpty());
        assertEquals(2, result.getFailures().size());
    }

    @Test
    void updateOrdersStatus_WithEmptyList_ReturnsEmptyResult() {
        List<BulkOrderStatusUpdateDTO> updates = List.of();

        BulkOrderUpdateResultDTO result = orderService.updateOrdersStatus(updates);

        assertTrue(result.getSuccesses().isEmpty());
        assertTrue(result.getFailures().isEmpty());
    }
}
