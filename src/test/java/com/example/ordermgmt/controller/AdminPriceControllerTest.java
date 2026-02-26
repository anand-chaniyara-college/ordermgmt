package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.AdminPricingDTO;
import com.example.ordermgmt.dto.AdminPricingWrapperDTO;
import com.example.ordermgmt.service.AdminPriceService;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminPriceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminPriceService adminPriceService;

    @InjectMocks
    private AdminPriceController adminPriceController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminPriceController)
                .setControllerAdvice(new com.example.ordermgmt.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetPrices_Success_NoParams() throws Exception {
        AdminPricingDTO priceDTO = new AdminPricingDTO();
        priceDTO.setItemId(UUID.randomUUID());
        priceDTO.setUnitPrice(BigDecimal.valueOf(100.50));

        when(adminPriceService.getAllPrices()).thenReturn(Collections.singletonList(priceDTO));

        mockMvc.perform(get("/api/admin/prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prices").isArray())
                .andExpect(jsonPath("$.prices[0].unitPrice").value(100.50));

        verify(adminPriceService, times(1)).getAllPrices();
    }

    @Test
    void testGetPrices_Success_WithItemId() throws Exception {
        UUID itemId = UUID.randomUUID();
        AdminPricingDTO priceDTO = new AdminPricingDTO();
        priceDTO.setItemId(itemId);
        priceDTO.setUnitPrice(BigDecimal.valueOf(200.00));

        when(adminPriceService.getPrice(itemId)).thenReturn(priceDTO);

        mockMvc.perform(get("/api/admin/prices")
                .param("itemId", itemId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(itemId.toString()))
                .andExpect(jsonPath("$.unitPrice").value(200.00));

        verify(adminPriceService, times(1)).getPrice(itemId);
    }

    @Test
    void testGetPrices_Success_WithPagination() throws Exception {
        AdminPricingDTO priceDTO = new AdminPricingDTO();
        priceDTO.setItemId(UUID.randomUUID());
        priceDTO.setUnitPrice(BigDecimal.valueOf(150.0));

        Page<AdminPricingDTO> pageResult = new PageImpl<>(Collections.singletonList(priceDTO), PageRequest.of(0, 5), 1);

        when(adminPriceService.getAllPrices(any(Pageable.class))).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/prices")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].unitPrice").value(150.0));

        verify(adminPriceService, times(1)).getAllPrices(any(Pageable.class));
    }

    @Test
    void testGetPrices_InternalServerError() throws Exception {
        when(adminPriceService.getAllPrices()).thenThrow(new RuntimeException("DB Exception"));

        mockMvc.perform(get("/api/admin/prices"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testAddPrices_Success() throws Exception {
        AdminPricingWrapperDTO wrapperDTO = new AdminPricingWrapperDTO();
        AdminPricingDTO priceDTO = new AdminPricingDTO();
        priceDTO.setItemId(UUID.randomUUID());
        priceDTO.setUnitPrice(BigDecimal.valueOf(99.99));
        wrapperDTO.setPrice(Collections.singletonList(priceDTO));

        doNothing().when(adminPriceService).addPrices(anyList());

        mockMvc.perform(post("/api/admin/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrapperDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Price records added successfully."));

        verify(adminPriceService, times(1)).addPrices(anyList());
    }

    @Test
    void testUpdatePrices_Success() throws Exception {
        AdminPricingWrapperDTO wrapperDTO = new AdminPricingWrapperDTO();
        AdminPricingDTO priceDTO = new AdminPricingDTO();
        priceDTO.setItemId(UUID.randomUUID());
        priceDTO.setUnitPrice(BigDecimal.valueOf(89.99));
        wrapperDTO.setPrice(Collections.singletonList(priceDTO));

        doNothing().when(adminPriceService).updatePrices(anyList());

        mockMvc.perform(put("/api/admin/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrapperDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Prices updated successfully."));

        verify(adminPriceService, times(1)).updatePrices(anyList());
    }

    @Test
    void testAddPrices_BadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json payload"))
                .andExpect(status().isBadRequest());
    }
}
