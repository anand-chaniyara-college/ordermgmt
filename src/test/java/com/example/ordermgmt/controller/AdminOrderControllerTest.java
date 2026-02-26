package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.BulkOrderStatusUpdateDTO;
import com.example.ordermgmt.dto.BulkOrderStatusUpdateWrapperDTO;
import com.example.ordermgmt.dto.BulkOrderUpdateResultDTO;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private AdminOrderController adminOrderController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminOrderController)
                .setControllerAdvice(new com.example.ordermgmt.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetAllOrders_Success_NoParams() throws Exception {
        OrderDTO order1 = new OrderDTO();
        order1.setOrderId(UUID.randomUUID());
        order1.setStatus("PENDING");

        when(orderService.getAllOrders()).thenReturn(Collections.singletonList(order1));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders[0].status").value("PENDING"));

        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    void testGetAllOrders_Success_WithOrderId() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderDTO order = new OrderDTO();
        order.setOrderId(orderId);
        order.setStatus("COMPLETED");

        when(orderService.getOrderById(orderId)).thenReturn(order);

        mockMvc.perform(get("/api/admin/orders")
                .param("orderId", orderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders[0].orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.orders[0].status").value("COMPLETED"));

        verify(orderService, times(1)).getOrderById(orderId);
    }

    @Test
    void testGetAllOrders_Success_WithPagination() throws Exception {
        OrderDTO order1 = new OrderDTO();
        order1.setOrderId(UUID.randomUUID());
        order1.setStatus("SHIPPED");

        Page<OrderDTO> pageResult = new PageImpl<>(Collections.singletonList(order1), PageRequest.of(0, 5), 1);

        when(orderService.getAllOrders(any(Pageable.class))).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/orders")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("SHIPPED"));

        verify(orderService, times(1)).getAllOrders(any(Pageable.class));
    }

    @Test
    void testUpdateOrderStatusBulk_Success() throws Exception {
        BulkOrderStatusUpdateWrapperDTO wrapper = new BulkOrderStatusUpdateWrapperDTO();
        BulkOrderStatusUpdateDTO update1 = new BulkOrderStatusUpdateDTO();
        update1.setOrderId(UUID.randomUUID());
        update1.setNewStatus("SHIPPED");
        wrapper.setOrders(Collections.singletonList(update1));

        BulkOrderUpdateResultDTO resultDTO = new BulkOrderUpdateResultDTO();
        OrderDTO successResult = new OrderDTO();
        successResult.setOrderId(update1.getOrderId());
        successResult.setStatus("SHIPPED");
        resultDTO.setSuccesses(Collections.singletonList(successResult));
        resultDTO.setFailures(Collections.emptyList());

        when(orderService.updateOrdersStatus(anyList())).thenReturn(resultDTO);

        mockMvc.perform(put("/api/admin/orders/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successes").isArray())
                .andExpect(jsonPath("$.failures").isArray());

        verify(orderService, times(1)).updateOrdersStatus(anyList());
    }

    @Test
    void testUpdateOrderStatusBulk_BadRequest_InvalidJson() throws Exception {
        mockMvc.perform(put("/api/admin/orders/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllOrders_InternalServerError() throws Exception {
        when(orderService.getAllOrders()).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isInternalServerError());
    }
}
