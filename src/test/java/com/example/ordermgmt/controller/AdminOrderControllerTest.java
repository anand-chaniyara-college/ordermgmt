package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.BulkOrderStatusUpdateDTO;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private AdminOrderController adminOrderController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminOrderController).build();
    }

    @Test
    void getAllOrders_NoParams_ReturnsList() throws Exception {
        OrderDTO order = new OrderDTO();
        order.setOrderId("order-1");
        List<OrderDTO> orders = Collections.singletonList(order);

        when(orderService.getAllOrders()).thenReturn(orders);

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("order-1"));
    }

    @Test
    void getAllOrders_WithOrderId_ReturnsListWithOneElement() throws Exception {
        OrderDTO order = new OrderDTO();
        order.setOrderId("order-123");

        when(orderService.getOrderById("order-123")).thenReturn(order);

        mockMvc.perform(get("/api/admin/orders")
                .param("orderId", "order-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("order-123"));
    }

    @Test
    void getAllOrders_WithPagination_CallsServiceWithPageable() throws Exception {
        when(orderService.getAllOrders(any(Pageable.class))).thenReturn(null);

        mockMvc.perform(get("/api/admin/orders")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void updateOrdersStatus_BulkUpdate_Success() throws Exception {
        BulkOrderStatusUpdateDTO update1 = new BulkOrderStatusUpdateDTO("order-1", "SHIPPED");
        List<BulkOrderStatusUpdateDTO> updates = Collections.singletonList(update1);

        OrderDTO order1 = new OrderDTO();
        order1.setOrderId("order-1");
        order1.setStatus("SHIPPED");

        BulkOrderUpdateResultDTO result = new BulkOrderUpdateResultDTO(
                Collections.singletonList(order1), Collections.emptyList());

        when(orderService.updateOrdersStatus(any())).thenReturn(result);

        mockMvc.perform(put("/api/admin/orders/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successes[0].orderId").value("order-1"))
                .andExpect(jsonPath("$.successes[0].status").value("SHIPPED"))
                .andExpect(jsonPath("$.failures").isEmpty());
    }

    // List validation requires @Validated AOP which is not available in standalone
    // MockMvc
    // void updateOrdersStatus_BulkUpdate_InvalidInput_ReturnsBadRequest() ...
}
