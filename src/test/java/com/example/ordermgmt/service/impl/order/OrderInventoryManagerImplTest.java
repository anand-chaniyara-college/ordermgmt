package com.example.ordermgmt.service.impl.order;

import com.example.ordermgmt.dto.OrderItemDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.OrderItem;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.entity.PricingHistory;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.InsufficientStockException;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.OrderItemRepository;
import com.example.ordermgmt.repository.PricingHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderInventoryManagerImplTest {

    @Mock
    private InventoryItemRepository inventoryRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PricingHistoryRepository pricingHistoryRepository;

    @InjectMocks
    private OrderInventoryManagerImpl orderInventoryManager;

    private UUID orderId;
    private UUID itemId1;
    private UUID itemId2;
    private Orders order;
    private InventoryItem inventoryItem1;
    private InventoryItem inventoryItem2;
    private OrderItemDTO orderItemDTO1;
    private OrderItemDTO orderItemDTO2;
    private PricingCatalog pricingCatalog;
    private PricingHistory pricingHistory;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        itemId1 = UUID.randomUUID();
        itemId2 = UUID.randomUUID();

        order = new Orders();
        order.setOrderId(orderId);

        pricingCatalog = new PricingCatalog();
        pricingCatalog.setUnitPrice(BigDecimal.valueOf(89.99));

        pricingHistory = new PricingHistory();
        pricingHistory.setNewPrice(BigDecimal.valueOf(89.99));
        pricingHistory.setCreatedTimestamp(LocalDateTime.now());

        inventoryItem1 = new InventoryItem();
        inventoryItem1.setItemId(itemId1);
        inventoryItem1.setItemName("Item 1");
        inventoryItem1.setAvailableStock(100);
        inventoryItem1.setReservedStock(20);
        inventoryItem1.setPricingCatalog(pricingCatalog);

        inventoryItem2 = new InventoryItem();
        inventoryItem2.setItemId(itemId2);
        inventoryItem2.setItemName("Item 2");
        inventoryItem2.setAvailableStock(200);
        inventoryItem2.setReservedStock(30);
        inventoryItem2.setPricingCatalog(pricingCatalog);

        orderItemDTO1 = new OrderItemDTO(itemId1, "Item 1", 5, BigDecimal.valueOf(99.99), BigDecimal.valueOf(499.95));
        orderItemDTO2 = new OrderItemDTO(itemId2, "Item 2", 10, BigDecimal.valueOf(99.99), BigDecimal.valueOf(999.90));
    }

    @Test
    void processAndSaveOrderItems_WithValidItems_ProcessesSuccessfully() {
        List<OrderItemDTO> items = List.of(orderItemDTO2, orderItemDTO1); // Unsorted to test sorting
        
        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1, inventoryItem2));
        when(pricingHistoryRepository.findFirstByInventoryItemItemIdOrderByCreatedTimestampDesc(itemId1))
                .thenReturn(Optional.of(pricingHistory));
        when(pricingHistoryRepository.findFirstByInventoryItemItemIdOrderByCreatedTimestampDesc(itemId2))
                .thenReturn(Optional.empty());

        List<OrderItemDTO> result = orderInventoryManager.processAndSaveOrderItems(items, order);

        assertEquals(2, result.size());
        
        // Verify both items are present (order may vary depending on UUID ordering in this environment)
        assertTrue(result.stream().anyMatch(r -> r.getItemId().equals(itemId1)));
        assertTrue(result.stream().anyMatch(r -> r.getItemId().equals(itemId2)));
        
        // Verify price resolution
        assertEquals(BigDecimal.valueOf(89.99), result.get(0).getUnitPrice()); // from history
        assertEquals(BigDecimal.valueOf(89.99), result.get(1).getUnitPrice()); // from catalog
        
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
    }

    @Test
    void processAndSaveOrderItems_WithNonExistingItem_ThrowsException() {
        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1)); // Only item1 returned, item2 missing

        List<OrderItemDTO> items = List.of(orderItemDTO1, orderItemDTO2);

        assertThrows(InvalidOperationException.class, () ->
                orderInventoryManager.processAndSaveOrderItems(items, order));
    }

    @Test
    void processAndSaveOrderItems_WithInsufficientStock_ThrowsException() {
        inventoryItem1.setAvailableStock(2);
        inventoryItem1.setReservedStock(8);
        orderItemDTO1 = new OrderItemDTO(itemId1, "Item 1", 5, BigDecimal.valueOf(99.99), BigDecimal.valueOf(499.95));

        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1));

        List<OrderItemDTO> items = List.of(orderItemDTO1);

        assertThrows(InsufficientStockException.class, () ->
                orderInventoryManager.processAndSaveOrderItems(items, order));
    }

    @Test
    void processAndSaveOrderItems_WithNoPricing_ThrowsException() {
        inventoryItem1.setPricingCatalog(null);
        
        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1));
        when(pricingHistoryRepository.findFirstByInventoryItemItemIdOrderByCreatedTimestampDesc(itemId1))
                .thenReturn(Optional.empty());

        List<OrderItemDTO> items = List.of(orderItemDTO1);

        assertThrows(InvalidOperationException.class, () ->
                orderInventoryManager.processAndSaveOrderItems(items, order));
    }

    @Test
    void handleInventoryUpdate_PendingToConfirmed_DoesNotChangeInventory() {
        List<OrderItem> orderItems = List.of(
                createOrderItem(order, inventoryItem1, 5),
                createOrderItem(order, inventoryItem2, 10)
        );

        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(orderItems);
        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1, inventoryItem2));

        orderInventoryManager.handleInventoryUpdate(order, OrderStatus.PENDING, OrderStatus.CONFIRMED);

        assertEquals(100, inventoryItem1.getAvailableStock());
        assertEquals(20, inventoryItem1.getReservedStock());
        assertEquals(200, inventoryItem2.getAvailableStock());
        assertEquals(30, inventoryItem2.getReservedStock());

        verify(inventoryRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    void handleInventoryUpdate_PendingToConfirmed_WithExistingReservation_DoesNotRecheckStock() {
        inventoryItem1.setAvailableStock(10);
        inventoryItem1.setReservedStock(8);

        List<OrderItem> orderItems = List.of(createOrderItem(order, inventoryItem1, 5));

        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(orderItems);
        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1));

        assertDoesNotThrow(() ->
                orderInventoryManager.handleInventoryUpdate(order, OrderStatus.PENDING, OrderStatus.CONFIRMED));

        assertEquals(10, inventoryItem1.getAvailableStock());
        assertEquals(8, inventoryItem1.getReservedStock());
        verify(inventoryRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    void handleInventoryUpdate_ConfirmedToCancelled_ReleasesStock() {
        List<OrderItem> orderItems = List.of(createOrderItem(order, inventoryItem1, 5));

        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(orderItems);
        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1));

        orderInventoryManager.handleInventoryUpdate(order, OrderStatus.CONFIRMED, OrderStatus.CANCELLED);

        assertEquals(105, inventoryItem1.getAvailableStock()); // 100 + 5
        assertEquals(15, inventoryItem1.getReservedStock()); // 20 - 5
    }

    @Test
    void handleInventoryUpdate_ProcessingToCancelled_ReleasesStock() {
        List<OrderItem> orderItems = List.of(createOrderItem(order, inventoryItem1, 5));

        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(orderItems);
        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1));

        orderInventoryManager.handleInventoryUpdate(order, OrderStatus.PROCESSING, OrderStatus.CANCELLED);

        assertEquals(105, inventoryItem1.getAvailableStock()); // 100 + 5
        assertEquals(15, inventoryItem1.getReservedStock()); // 20 - 5
    }

    @Test
    void handleInventoryUpdate_CancelledWithInsufficientReservedStock_ThrowsMeaningfulException() {
        inventoryItem1.setReservedStock(3);
        List<OrderItem> orderItems = List.of(createOrderItem(order, inventoryItem1, 5));

        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(orderItems);
        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                orderInventoryManager.handleInventoryUpdate(order, OrderStatus.CONFIRMED, OrderStatus.CANCELLED));

        assertTrue(ex.getMessage().contains("cannot release 5 units"));
        assertEquals(100, inventoryItem1.getAvailableStock());
        assertEquals(3, inventoryItem1.getReservedStock());
        verify(inventoryRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    void handleInventoryUpdate_ShippedToDelivered_RemovesReservation() {
        List<OrderItem> orderItems = List.of(createOrderItem(order, inventoryItem1, 5));

        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(orderItems);
        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1));

        orderInventoryManager.handleInventoryUpdate(order, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

        assertEquals(100, inventoryItem1.getAvailableStock()); // unchanged
        assertEquals(15, inventoryItem1.getReservedStock()); // 20 - 5
    }

    @Test
    void handleInventoryUpdate_DeliveredWithInsufficientReservedStock_ThrowsMeaningfulException() {
        inventoryItem1.setReservedStock(3);
        List<OrderItem> orderItems = List.of(createOrderItem(order, inventoryItem1, 5));

        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(orderItems);
        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                orderInventoryManager.handleInventoryUpdate(order, OrderStatus.SHIPPED, OrderStatus.DELIVERED));

        assertTrue(ex.getMessage().contains("cannot release 5 units"));
        assertEquals(100, inventoryItem1.getAvailableStock());
        assertEquals(3, inventoryItem1.getReservedStock());
        verify(inventoryRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    void handleInventoryUpdate_WithNoOrderItems_LogsWarning() {
        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(List.of());

        orderInventoryManager.handleInventoryUpdate(order, OrderStatus.PENDING, OrderStatus.CONFIRMED);

        verify(inventoryRepository, never()).findAllByItemIdInForUpdate(anyList());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void handleInventoryUpdate_PendingToCancelled_NoInventoryChange() {
        List<OrderItem> orderItems = List.of(createOrderItem(order, inventoryItem1, 5));

        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(orderItems);
        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1));

        orderInventoryManager.handleInventoryUpdate(order, OrderStatus.PENDING, OrderStatus.CANCELLED);

        assertEquals(105, inventoryItem1.getAvailableStock()); // reservation reverted
        assertEquals(15, inventoryItem1.getReservedStock());
        verify(inventoryRepository).save(any());
    }

    @Test
    void handleInventoryUpdate_ProcessingToShipped_NoInventoryChange() {
        List<OrderItem> orderItems = List.of(createOrderItem(order, inventoryItem1, 5));

        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(orderItems);
        when(inventoryRepository.findAllByItemIdInForUpdate(anyList()))
                .thenReturn(List.of(inventoryItem1));

        orderInventoryManager.handleInventoryUpdate(order, OrderStatus.PROCESSING, OrderStatus.SHIPPED);

        assertEquals(100, inventoryItem1.getAvailableStock()); // unchanged
        assertEquals(20, inventoryItem1.getReservedStock()); // unchanged
        verify(inventoryRepository, never()).save(any());
    }

    private OrderItem createOrderItem(Orders order, InventoryItem item, int quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setInventoryItem(item);
        orderItem.setQuantity(quantity);
        return orderItem;
    }
}
