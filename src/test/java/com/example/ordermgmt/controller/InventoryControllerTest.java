package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.AddStockRequestDTO;
import com.example.ordermgmt.dto.AddStockWrapperDTO;
import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.dto.InventoryItemWrapperDTO;
import com.example.ordermgmt.service.InventoryService;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                .setControllerAdvice(new com.example.ordermgmt.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetInventory_Success_NoParams() throws Exception {
        InventoryItemDTO item = new InventoryItemDTO();
        item.setItemName("Test Item");

        when(inventoryService.getAllInventory()).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/api/admin/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventory").isArray())
                .andExpect(jsonPath("$.inventory[0].itemName").value("Test Item"));

        verify(inventoryService, times(1)).getAllInventory();
    }

    @Test
    void testGetInventory_Success_WithItemId() throws Exception {
        UUID itemId = UUID.randomUUID();
        InventoryItemDTO item = new InventoryItemDTO();
        item.setItemId(itemId);
        item.setItemName("Specific Item");

        when(inventoryService.getInventoryItem(itemId)).thenReturn(item);

        mockMvc.perform(get("/api/admin/inventory")
                .param("itemId", itemId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName").value("Specific Item"));

        verify(inventoryService, times(1)).getInventoryItem(itemId);
    }

    @Test
    void testGetInventory_Success_WithPagination() throws Exception {
        InventoryItemDTO item = new InventoryItemDTO();
        item.setItemName("Paged Item");

        Page<InventoryItemDTO> pageResult = new PageImpl<>(Collections.singletonList(item), PageRequest.of(0, 5), 1);

        when(inventoryService.getAllInventory(any(Pageable.class))).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/inventory")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].itemName").value("Paged Item"));

        verify(inventoryService, times(1)).getAllInventory(any(Pageable.class));
    }

    @Test
    void testAddInventoryItems_Success() throws Exception {
        InventoryItemWrapperDTO wrapper = new InventoryItemWrapperDTO();
        InventoryItemDTO item = new InventoryItemDTO();
        item.setItemName("New Item");
        item.setAvailableStock(100);
        wrapper.setInventory(Collections.singletonList(item));

        UUID createdId = UUID.randomUUID();
        when(inventoryService.addInventoryItems(anyList())).thenReturn(Collections.singletonList(createdId));

        mockMvc.perform(post("/api/admin/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0]").value(createdId.toString()));

        verify(inventoryService, times(1)).addInventoryItems(anyList());
    }

    @Test
    void testAddStock_Success() throws Exception {
        AddStockWrapperDTO wrapper = new AddStockWrapperDTO();
        AddStockRequestDTO addStock = new AddStockRequestDTO();
        addStock.setItemId(UUID.randomUUID());
        addStock.setAddStock(50);
        wrapper.setAddstock(Collections.singletonList(addStock));

        UUID updatedId = addStock.getItemId();
        when(inventoryService.addStock(anyList())).thenReturn(Collections.singletonList(updatedId));

        mockMvc.perform(post("/api/admin/inventory/addstock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0]").value(updatedId.toString()));

        verify(inventoryService, times(1)).addStock(anyList());
    }

    @Test
    void testUpdateInventoryItems_Success() throws Exception {
        InventoryItemWrapperDTO wrapper = new InventoryItemWrapperDTO();
        InventoryItemDTO item = new InventoryItemDTO();
        item.setItemId(UUID.randomUUID());
        item.setItemName("Updated Item");
        item.setAvailableStock(10);
        wrapper.setInventory(Collections.singletonList(item));

        UUID updatedId = item.getItemId();
        when(inventoryService.updateInventoryItems(anyList())).thenReturn(Collections.singletonList(updatedId));

        mockMvc.perform(put("/api/admin/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0]").value(updatedId.toString()));

        verify(inventoryService, times(1)).updateInventoryItems(anyList());
    }

    @Test
    void testDeleteInventoryItems_Success() throws Exception {
        UUID id = UUID.randomUUID();
        List<UUID> ids = Collections.singletonList(id);

        doNothing().when(inventoryService).deleteInventoryItems(ids);

        mockMvc.perform(delete("/api/admin/inventory/" + id))
                .andExpect(status().isNoContent());

        verify(inventoryService, times(1)).deleteInventoryItems(ids);
    }

    @Test
    void testGetInventory_InternalServerError() throws Exception {
        when(inventoryService.getAllInventory()).thenThrow(new RuntimeException("DB Error"));

        mockMvc.perform(get("/api/admin/inventory"))
                .andExpect(status().isInternalServerError());
    }
}
