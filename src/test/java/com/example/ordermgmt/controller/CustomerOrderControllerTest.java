package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private CustomerOrderController customerOrderController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerOrderController)
                .setControllerAdvice(new com.example.ordermgmt.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateOrder_Success() throws Exception {
        OrderDTO requestDTO = new OrderDTO();
        com.example.ordermgmt.dto.OrderItemDTO orderItem1 = new com.example.ordermgmt.dto.OrderItemDTO();
        orderItem1.setItemId(java.util.UUID.randomUUID());
        orderItem1.setQuantity(1);
        requestDTO.setItems(java.util.Collections.singletonList(orderItem1));

        OrderDTO responseDTO = new OrderDTO();
        responseDTO.setOrderId(UUID.randomUUID());
        responseDTO.setStatus("PENDING");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("customer@example.com");

        when(orderService.createOrder(any(OrderDTO.class), eq("customer@example.com"))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/customer/orders")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService, times(1)).createOrder(any(OrderDTO.class), eq("customer@example.com"));
    }

    @Test
    void testGetMyOrders_Success_NoParams() throws Exception {
        OrderDTO order = new OrderDTO();
        order.setStatus("COMPLETED");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("customer@example.com");

        when(orderService.getCustomerOrders("customer@example.com")).thenReturn(Collections.singletonList(order));

        mockMvc.perform(get("/api/customer/orders")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders[0].status").value("COMPLETED"));

        verify(orderService, times(1)).getCustomerOrders("customer@example.com");
    }

    @Test
    void testGetMyOrders_Success_WithOrderId() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderDTO order = new OrderDTO();
        order.setStatus("SHIPPED");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("customer@example.com");

        when(orderService.getCustomerOrderById(orderId, "customer@example.com")).thenReturn(order);

        mockMvc.perform(get("/api/customer/orders")
                .principal(authentication)
                .param("orderId", orderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders[0].status").value("SHIPPED"));

        verify(orderService, times(1)).getCustomerOrderById(orderId, "customer@example.com");
    }

    @Test
    void testGetMyOrders_Success_WithPagination() throws Exception {
        OrderDTO order = new OrderDTO();
        order.setStatus("PENDING");

        Page<OrderDTO> pageResult = new PageImpl<>(Collections.singletonList(order), PageRequest.of(0, 5), 1);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("customer@example.com");

        when(orderService.getCustomerOrders(eq("customer@example.com"), any(Pageable.class))).thenReturn(pageResult);

        mockMvc.perform(get("/api/customer/orders")
                .principal(authentication)
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        verify(orderService, times(1)).getCustomerOrders(eq("customer@example.com"), any(Pageable.class));
    }

    @Test
    void testCancelMyOrder_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderDTO cancelledOrder = new OrderDTO();
        cancelledOrder.setStatus("CANCELLED");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("customer@example.com");

        when(orderService.cancelOrder(orderId, "customer@example.com")).thenReturn(cancelledOrder);

        mockMvc.perform(put("/api/customer/orders/" + orderId + "/cancel")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService, times(1)).cancelOrder(orderId, "customer@example.com");
    }

    @Test
    void testCreateOrder_InternalServerError() throws Exception {
        OrderDTO requestDTO = new OrderDTO();
        com.example.ordermgmt.dto.OrderItemDTO orderItem2 = new com.example.ordermgmt.dto.OrderItemDTO();
        orderItem2.setItemId(java.util.UUID.randomUUID());
        orderItem2.setQuantity(1);
        requestDTO.setItems(java.util.Collections.singletonList(orderItem2));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("customer@example.com");

        when(orderService.createOrder(any(OrderDTO.class), eq("customer@example.com")))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/customer/orders")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isInternalServerError());
    }
}
