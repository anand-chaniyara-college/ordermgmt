package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.ResourceNotFoundException;
import com.example.ordermgmt.repository.InventoryItemRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceImplTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    public void testGetAllInventory() {
        // Setup
        InventoryItem item1 = new InventoryItem();
        item1.setItemId("ITEM-001");
        item1.setItemName("Item 1");

        InventoryItem item2 = new InventoryItem();
        item2.setItemId("ITEM-002");
        item2.setItemName("Item 2");

        when(inventoryItemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

        // Execute
        List<InventoryItemDTO> result = inventoryService.getAllInventory();

        // Verify
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("ITEM-001", result.get(0).getItemId());
        Assertions.assertEquals("ITEM-002", result.get(1).getItemId());
    }

    @Test
    public void testGetInventoryItem_Success() {
        // Setup
        String itemId = "ITEM-001";
        InventoryItem item = new InventoryItem();
        item.setItemId(itemId);
        item.setItemName("Item 1");
        item.setAvailableStock(10);
        item.setReservedStock(2);

        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        // Execute
        InventoryItemDTO result = inventoryService.getInventoryItem(itemId);

        // Verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(itemId, result.getItemId());
        Assertions.assertEquals(10, result.getAvailableStock());
    }

    @Test
    public void testGetInventoryItem_NotFound() {
        // Setup
        String itemId = "ITEM-999";
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.empty());

        // Execute & Verify
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            inventoryService.getInventoryItem(itemId);
        });
    }

    @Test
    public void testAddInventoryItem_Success() {
        // Setup
        // Name is required for creation
        InventoryItemDTO dto = new InventoryItemDTO("ITEM-001", "New Item", 100, 0);
        when(inventoryItemRepository.existsById(dto.getItemId())).thenReturn(false);

        // Execute
        List<String> result = inventoryService.addInventoryItems(Collections.singletonList(dto));

        // Verify
        verify(inventoryItemRepository, times(1)).saveAll(any());
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("ITEM-001", result.get(0));
    }

    @Test
    public void testAddInventoryItem_MissingName_ThrowsException() {
        // Setup
        // null Name should fail creation
        InventoryItemDTO dto = new InventoryItemDTO("ITEM-001", null, 100, 0);

        // Execute & Verify
        Assertions.assertThrows(InvalidOperationException.class, () -> {
            inventoryService.addInventoryItems(Collections.singletonList(dto));
        });
        verify(inventoryItemRepository, never()).saveAll(any());
    }

    @Test
    public void testAddInventoryItem_AlreadyExists() {
        // Setup
        InventoryItemDTO dto = new InventoryItemDTO("ITEM-001", "Existing Item", 100, 0);
        when(inventoryItemRepository.existsById(dto.getItemId())).thenReturn(true);

        // Execute & Verify
        Assertions.assertThrows(InvalidOperationException.class, () -> {
            inventoryService.addInventoryItems(Collections.singletonList(dto));
        });
        verify(inventoryItemRepository, never()).saveAll(any());
    }

    @Test
    public void testUpdateInventoryItem_Success_WithNewName() {
        // Setup
        String itemId = "ITEM-001";
        InventoryItemDTO dto = new InventoryItemDTO(itemId, "Updated Name", 50, 5);
        InventoryItem existingItem = new InventoryItem();
        existingItem.setItemId(itemId);
        existingItem.setItemName("Old Name");
        existingItem.setReservedStock(0);

        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));

        // Execute
        List<String> result = inventoryService.updateInventoryItems(Collections.singletonList(dto));

        // Verify
        Assertions.assertEquals("Updated Name", existingItem.getItemName());
        Assertions.assertEquals(50, existingItem.getAvailableStock());
        Assertions.assertEquals(0, existingItem.getReservedStock()); // Reserved stock should remain 0
        verify(inventoryItemRepository, times(1)).saveAll(any());
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(itemId, result.get(0));
    }

    @Test
    public void testUpdateInventoryItem_Success_WithoutName() {
        // Setup
        String itemId = "ITEM-001";
        InventoryItemDTO dto = new InventoryItemDTO(itemId, null, 50, 5); // No name provided
        InventoryItem existingItem = new InventoryItem();
        existingItem.setItemId(itemId);
        existingItem.setItemName("Preserved Name"); // Should act as spy if needed, but simple POJO check works

        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));

        // Execute
        List<String> result = inventoryService.updateInventoryItems(Collections.singletonList(dto));

        // Verify
        Assertions.assertEquals("Preserved Name", existingItem.getItemName()); // Name should NOT change
        Assertions.assertEquals(50, existingItem.getAvailableStock());
        verify(inventoryItemRepository, times(1)).saveAll(any());
        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void testUpdateInventoryItem_NotFound() {
        // Setup
        String itemId = "ITEM-999";
        InventoryItemDTO dto = new InventoryItemDTO(itemId, "Updated Name", 50, 5);
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.empty());

        // Execute & Verify
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            inventoryService.updateInventoryItems(Collections.singletonList(dto));
        });
    }

    @Test
    public void testUpdateInventoryItem_IgnoreReservedStock() {
        // Setup
        String itemId = "ITEM-001";
        // DTO has reservedStock = 100, but it should be ignored
        InventoryItemDTO dto = new InventoryItemDTO(itemId, "Item 1", 50, 100);
        InventoryItem existingItem = new InventoryItem();
        existingItem.setItemId(itemId);
        existingItem.setItemName("Item 1");
        existingItem.setAvailableStock(10);
        existingItem.setReservedStock(5); // Existing reserved stock

        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));

        // Execute
        inventoryService.updateInventoryItems(Collections.singletonList(dto));

        // Verify
        Assertions.assertEquals(50, existingItem.getAvailableStock()); // Available stock updated
        Assertions.assertEquals(5, existingItem.getReservedStock()); // Reserved stock UNCHANGED (ignored 100)
        verify(inventoryItemRepository, times(1)).saveAll(any());
    }

    @Test
    public void testDeleteInventoryItem_Success() {
        // Setup
        String itemId = "ITEM-001";
        when(inventoryItemRepository.existsById(itemId)).thenReturn(true);

        // Execute
        inventoryService.deleteInventoryItems(Collections.singletonList(itemId));

        // Verify
        verify(inventoryItemRepository, times(1)).deleteAllById(any());
    }

    @Test
    public void testDeleteInventoryItem_NotFound() {
        // Setup
        String itemId = "ITEM-999";
        when(inventoryItemRepository.existsById(itemId)).thenReturn(false);

        // Execute & Verify
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            inventoryService.deleteInventoryItems(Collections.singletonList(itemId));
        });
    }

    @Test
    public void testAddStock_Success() {
        // Setup
        String itemId = "ITEM-001";
        com.example.ordermgmt.dto.AddStockRequestDTO request = new com.example.ordermgmt.dto.AddStockRequestDTO(itemId,
                10);
        InventoryItem existingItem = new InventoryItem();
        existingItem.setItemId(itemId);
        existingItem.setAvailableStock(20);

        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));

        // Execute
        List<String> result = inventoryService.addStock(Collections.singletonList(request));

        // Verify
        Assertions.assertEquals(30, existingItem.getAvailableStock());
        verify(inventoryItemRepository, times(1)).saveAll(any());
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(itemId, result.get(0));
    }

    @Test
    public void testAddStock_ItemNotFound() {
        // Setup
        String itemId = "ITEM-999";
        com.example.ordermgmt.dto.AddStockRequestDTO request = new com.example.ordermgmt.dto.AddStockRequestDTO(itemId,
                10);

        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.empty());

        // Execute & Verify
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            inventoryService.addStock(Collections.singletonList(request));
        });
        verify(inventoryItemRepository, never()).saveAll(any());
    }
}
