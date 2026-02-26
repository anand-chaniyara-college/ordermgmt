package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.ProductDTO;
import com.example.ordermgmt.service.ProductService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new com.example.ordermgmt.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetAvailableProducts_Success_NoParams() throws Exception {
        ProductDTO product = new ProductDTO();
        product.setItemId(UUID.randomUUID());
        product.setItemName("Laptop");
        product.setUnitPrice(BigDecimal.valueOf(999.99));

        when(productService.getAvailableProducts()).thenReturn(Collections.singletonList(product));

        mockMvc.perform(get("/api/customer/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products[0].itemName").value("Laptop"))
                .andExpect(jsonPath("$.products[0].unitPrice").value(999.99));

        verify(productService, times(1)).getAvailableProducts();
    }

    @Test
    void testGetAvailableProducts_Success_WithPagination() throws Exception {
        ProductDTO product = new ProductDTO();
        product.setItemId(UUID.randomUUID());
        product.setItemName("Smartphone");
        product.setUnitPrice(BigDecimal.valueOf(599.99));

        Page<ProductDTO> pageResult = new PageImpl<>(Collections.singletonList(product), PageRequest.of(0, 5), 1);

        when(productService.getAvailableProducts(any(Pageable.class))).thenReturn(pageResult);

        mockMvc.perform(get("/api/customer/products")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].itemName").value("Smartphone"))
                .andExpect(jsonPath("$.content[0].unitPrice").value(599.99));

        verify(productService, times(1)).getAvailableProducts(any(Pageable.class));
    }

    @Test
    void testGetAvailableProducts_InternalServerError() throws Exception {
        when(productService.getAvailableProducts()).thenThrow(new RuntimeException("DB Fetch Error"));

        mockMvc.perform(get("/api/customer/products"))
                .andExpect(status().isInternalServerError());
    }
}
