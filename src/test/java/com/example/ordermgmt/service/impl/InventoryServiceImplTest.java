package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.AddStockRequestDTO;
import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.ResourceNotFoundException;
import com.example.ordermgmt.repository.InventoryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private UUID itemId;
    private InventoryItem inventoryItem;
    private InventoryItemDTO inventoryItemDTO;

    @BeforeEach
    void setUp() {
        itemId = UUID.randomUUID();

        inventoryItem = new InventoryItem();
        inventoryItem.setItemId(itemId);
        inventoryItem.setItemName("Test Item");
        inventoryItem.setAvailableStock(100);
        inventoryItem.setReservedStock(10);

        inventoryItemDTO = new InventoryItemDTO(itemId, "Test Item", 100, 10);
    }

    @Test
    void getAllInventory_AsList_ReturnsAllItems() {
        when(inventoryItemRepository.findAll()).thenReturn(List.of(inventoryItem));

        List<InventoryItemDTO> result = inventoryService.getAllInventory();

        assertEquals(1, result.size());
        assertEquals(itemId, result.get(0).getItemId());
        assertEquals("Test Item", result.get(0).getItemName());
        assertEquals(100, result.get(0).getAvailableStock());
    }

    @Test
    void getAllInventory_AsPage_ReturnsPagedItems() {
        Page<InventoryItem> itemPage = new PageImpl<>(List.of(inventoryItem));
        when(inventoryItemRepository.findAll(any(Pageable.class))).thenReturn(itemPage);

        Page<InventoryItemDTO> result = inventoryService.getAllInventory(PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals(itemId, result.getContent().get(0).getItemId());
    }

    @Test
    void getInventoryItem_WithExistingId_ReturnsItem() {
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(inventoryItem));

        InventoryItemDTO result = inventoryService.getInventoryItem(itemId);

        assertNotNull(result);
        assertEquals(itemId, result.getItemId());
        assertEquals("Test Item", result.getItemName());
    }

    @Test
    void getInventoryItem_WithNonExistingId_ThrowsException() {
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> inventoryService.getInventoryItem(itemId));
    }

    @Test
    void addInventoryItems_WithValidItems_AddsSuccessfully() {
        InventoryItemDTO newItem = new InventoryItemDTO(null, "New Item", 50, 5);
        InventoryItem savedItem = new InventoryItem();
        savedItem.setItemId(UUID.randomUUID());
        savedItem.setItemName("New Item");

        when(inventoryItemRepository.saveAll(anyList())).thenReturn(List.of(savedItem));

        List<UUID> result = inventoryService.addInventoryItems(List.of(newItem));

        assertEquals(1, result.size());
        verify(inventoryItemRepository).saveAll(anyList());
    }

    @Test
    void addInventoryItems_WithNullItemName_ThrowsException() {
        InventoryItemDTO invalidItem = new InventoryItemDTO(null, null, 50, 5);

        assertThrows(InvalidOperationException.class, () -> 
                inventoryService.addInventoryItems(List.of(invalidItem)));
    }

    @Test
    void addInventoryItems_WithEmptyItemName_ThrowsException() {
        InventoryItemDTO invalidItem = new InventoryItemDTO(null, "", 50, 5);

        assertThrows(InvalidOperationException.class, () -> 
                inventoryService.addInventoryItems(List.of(invalidItem)));
    }

    @Test
    void addInventoryItems_WithNullReservedStock_SetsDefaultToZero() {
        InventoryItemDTO newItem = new InventoryItemDTO(null, "New Item", 50, null);
        
        when(inventoryItemRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<InventoryItem> items = invocation.getArgument(0);
            items.get(0).setItemId(UUID.randomUUID());
            return items;
        });

        List<UUID> result = inventoryService.addInventoryItems(List.of(newItem));

        assertEquals(1, result.size());
        ArgumentCaptor<List<InventoryItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(inventoryItemRepository).saveAll(captor.capture());
        
        InventoryItem saved = captor.getValue().get(0);
        assertEquals(0, saved.getReservedStock());
    }

    @Test
    void updateInventoryItems_WithValidItems_UpdatesSuccessfully() {
        InventoryItemDTO updateDTO = new InventoryItemDTO(itemId, "Updated Name", 200, 20);
        
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(inventoryItem));

        List<UUID> result = inventoryService.updateInventoryItems(List.of(updateDTO));

        assertEquals(1, result.size());
        assertEquals(itemId, result.get(0));
        assertEquals("Updated Name", inventoryItem.getItemName());
        assertEquals(200, inventoryItem.getAvailableStock());
        verify(inventoryItemRepository).saveAll(anyList());
    }

    @Test
    void updateInventoryItems_WithNullItemId_ThrowsException() {
        InventoryItemDTO updateDTO = new InventoryItemDTO(null, "Updated Name", 200, 20);

        assertThrows(InvalidOperationException.class, () -> 
                inventoryService.updateInventoryItems(List.of(updateDTO)));
    }

    @Test
    void updateInventoryItems_WithNonExistingItem_ThrowsException() {
        InventoryItemDTO updateDTO = new InventoryItemDTO(itemId, "Updated Name", 200, 20);
        
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                inventoryService.updateInventoryItems(List.of(updateDTO)));
    }

    @Test
    void updateInventoryItems_WithNullItemName_KeepsExistingName() {
        InventoryItemDTO updateDTO = new InventoryItemDTO(itemId, null, 200, 20);
        
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(inventoryItem));

        inventoryService.updateInventoryItems(List.of(updateDTO));

        assertEquals("Test Item", inventoryItem.getItemName()); // unchanged
        assertEquals(200, inventoryItem.getAvailableStock()); // updated
    }

    @Test
    void deleteInventoryItems_WithExistingItems_DeletesSuccessfully() {
        when(inventoryItemRepository.existsById(itemId)).thenReturn(true);

        inventoryService.deleteInventoryItems(List.of(itemId));

        verify(inventoryItemRepository).deleteAllById(List.of(itemId));
    }

    @Test
    void deleteInventoryItems_WithNonExistingItem_ThrowsException() {
        when(inventoryItemRepository.existsById(itemId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> 
                inventoryService.deleteInventoryItems(List.of(itemId)));
    }

    @Test
    void addStock_WithValidItems_AddsStockSuccessfully() {
        AddStockRequestDTO addStockRequest = new AddStockRequestDTO(itemId, 50);
        
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(inventoryItem));

        List<UUID> result = inventoryService.addStock(List.of(addStockRequest));

        assertEquals(1, result.size());
        assertEquals(150, inventoryItem.getAvailableStock()); // 100 + 50
        verify(inventoryItemRepository).saveAll(anyList());
    }

    @Test
    void addStock_WithNonExistingItem_ThrowsException() {
        AddStockRequestDTO addStockRequest = new AddStockRequestDTO(itemId, 50);
        
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                inventoryService.addStock(List.of(addStockRequest)));
    }

    @Test
    void addStock_MultipleItems_UpdatesAll() {
        UUID itemId2 = UUID.randomUUID();
        InventoryItem item2 = new InventoryItem();
        item2.setItemId(itemId2);
        item2.setAvailableStock(200);
        
        AddStockRequestDTO request1 = new AddStockRequestDTO(itemId, 50);
        AddStockRequestDTO request2 = new AddStockRequestDTO(itemId2, 100);
        
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(inventoryItem));
        when(inventoryItemRepository.findById(itemId2)).thenReturn(Optional.of(item2));

        List<UUID> result = inventoryService.addStock(List.of(request1, request2));

        assertEquals(2, result.size());
        assertEquals(150, inventoryItem.getAvailableStock());
        assertEquals(300, item2.getAvailableStock());
    }
}
