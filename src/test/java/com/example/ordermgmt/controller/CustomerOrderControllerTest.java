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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(MockitoExtension.class)
class CustomerOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private CustomerOrderController customerOrderController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private Principal mockPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerOrderController).build();
        mockPrincipal = new UsernamePasswordAuthenticationToken("test@example.com", "password");
    }

    @Test
    void createOrder_Success() throws Exception {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId("order-123");
        com.example.ordermgmt.dto.OrderItemDTO item = new com.example.ordermgmt.dto.OrderItemDTO();
        item.setItemId("item-1");
        item.setQuantity(1);
        orderDTO.setItems(Collections.singletonList(item));

        when(orderService.createOrder(any(OrderDTO.class), eq("test@example.com"))).thenReturn(orderDTO);

        mockMvc.perform(post("/api/customer/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderDTO))
                .principal(mockPrincipal))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-123"));
    }

    @Test
    void getMyOrders_Success() throws Exception {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId("order-123");
        List<OrderDTO> orders = Collections.singletonList(orderDTO);

        when(orderService.getCustomerOrders("test@example.com")).thenReturn(orders);

        mockMvc.perform(get("/api/customer/orders")
                .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("order-123"));
    }
}
