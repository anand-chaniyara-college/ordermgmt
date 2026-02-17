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
        InventoryItemDTO dto = new InventoryItemDTO("ITEM-001", "New Item", 100, 0);
        when(inventoryItemRepository.existsById(dto.getItemId())).thenReturn(false);

        // Execute
        String result = inventoryService.addInventoryItem(dto);

        // Verify
        verify(inventoryItemRepository, times(1)).save(any(InventoryItem.class));
        Assertions.assertEquals("Item added successfully", result);
    }

    @Test
    public void testAddInventoryItem_AlreadyExists() {
        // Setup
        InventoryItemDTO dto = new InventoryItemDTO("ITEM-001", "Existing Item", 100, 0);
        when(inventoryItemRepository.existsById(dto.getItemId())).thenReturn(true);

        // Execute & Verify
        Assertions.assertThrows(InvalidOperationException.class, () -> {
            inventoryService.addInventoryItem(dto);
        });
        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    public void testUpdateInventoryItem_Success() {
        // Setup
        String itemId = "ITEM-001";
        InventoryItemDTO dto = new InventoryItemDTO(itemId, "Updated Name", 50, 5);
        InventoryItem existingItem = new InventoryItem();
        existingItem.setItemId(itemId);
        existingItem.setItemName("Old Name");

        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));

        // Execute
        String result = inventoryService.updateInventoryItem(itemId, dto);

        // Verify
        Assertions.assertEquals("Updated Name", existingItem.getItemName());
        Assertions.assertEquals(50, existingItem.getAvailableStock());
        verify(inventoryItemRepository, times(1)).save(existingItem);
        Assertions.assertEquals("Item updated successfully", result);
    }

    @Test
    public void testUpdateInventoryItem_NotFound() {
        // Setup
        String itemId = "ITEM-999";
        InventoryItemDTO dto = new InventoryItemDTO(itemId, "Updated Name", 50, 5);
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.empty());

        // Execute & Verify
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            inventoryService.updateInventoryItem(itemId, dto);
        });
    }

    @Test
    public void testUpdateInventoryItem_IdMismatch() {
        // Setup
        String itemId = "ITEM-001";
        InventoryItemDTO dto = new InventoryItemDTO("ITEM-002", "Updated Name", 50, 5);

        // Execute & Verify
        Assertions.assertThrows(InvalidOperationException.class, () -> {
            inventoryService.updateInventoryItem(itemId, dto);
        });
    }

    @Test
    public void testDeleteInventoryItem_Success() {
        // Setup
        String itemId = "ITEM-001";
        when(inventoryItemRepository.existsById(itemId)).thenReturn(true);

        // Execute
        String result = inventoryService.deleteInventoryItem(itemId);

        // Verify
        verify(inventoryItemRepository, times(1)).deleteById(itemId);
        Assertions.assertEquals("Item deleted successfully", result);
    }

    @Test
    public void testDeleteInventoryItem_NotFound() {
        // Setup
        String itemId = "ITEM-999";
        when(inventoryItemRepository.existsById(itemId)).thenReturn(false);

        // Execute & Verify
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            inventoryService.deleteInventoryItem(itemId);
        });
    }
}
