package com.example.ordermgmt.service.impl.order;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderItemDTO;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.OrderItem;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.entity.OrderStatusLookup;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OrderMapperImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderMapperImpl orderMapper;

    private UUID orderId;
    private UUID customerId;
    private UUID itemId1;
    private UUID itemId2;
    private Orders order;
    private Customer customer;
    private OrderStatusLookup status;
    private InventoryItem inventoryItem1;
    private InventoryItem inventoryItem2;
    private OrderItem orderItem1;
    private OrderItem orderItem2;
    private List<OrderItemDTO> itemDTOs;

    @BeforeEach
    void setUp() {
        orderId    = UUID.randomUUID();
        customerId = UUID.randomUUID();
        itemId1    = UUID.randomUUID();
        itemId2    = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerId(customerId);

        status = new OrderStatusLookup();
        status.setStatusName("PENDING");

        order = new Orders();
        order.setOrderId(orderId);
        order.setCustomer(customer);
        order.setStatus(status);
        order.setCreatedTimestamp(LocalDateTime.now());
        order.setUpdatedTimestamp(LocalDateTime.now());

        inventoryItem1 = new InventoryItem();
        inventoryItem1.setItemId(itemId1);
        inventoryItem1.setItemName("Item 1");

        inventoryItem2 = new InventoryItem();
        inventoryItem2.setItemId(itemId2);
        inventoryItem2.setItemName("Item 2");

        orderItem1 = new OrderItem();
        orderItem1.setOrder(order);
        orderItem1.setInventoryItem(inventoryItem1);
        orderItem1.setQuantity(5);
        orderItem1.setUnitPrice(BigDecimal.valueOf(99.99));

        orderItem2 = new OrderItem();
        orderItem2.setOrder(order);
        orderItem2.setInventoryItem(inventoryItem2);
        orderItem2.setQuantity(10);
        orderItem2.setUnitPrice(BigDecimal.valueOf(49.99));

        itemDTOs = List.of(
                new OrderItemDTO(itemId1, "Item 1", 5,  BigDecimal.valueOf(99.99), BigDecimal.valueOf(499.95)),
                new OrderItemDTO(itemId2, "Item 2", 10, BigDecimal.valueOf(49.99), BigDecimal.valueOf(499.90))
        );
    }

    // -------------------------------------------------------------------------
    // convertToDTO(Orders, List<OrderItemDTO>, BigDecimal) — full overload
    // -------------------------------------------------------------------------

    @Test
    void convertToDTO_WithItemsAndTotal_ReturnsCompleteDTO() {
        BigDecimal total = BigDecimal.valueOf(999.85);

        OrderDTO result = orderMapper.convertToDTO(order, itemDTOs, total);

        assertNotNull(result);
        assertEquals(orderId,    result.getOrderId());
        assertEquals(customerId, result.getCustomerId());
        assertEquals("PENDING",  result.getStatus());
        assertEquals(order.getCreatedTimestamp(), result.getCreatedTimestamp());
        assertEquals(order.getUpdatedTimestamp(),  result.getUpdatedTimestamp());
        assertEquals(2,     result.getItems().size());
        assertEquals(total, result.getTotalAmount());
    }

    // -------------------------------------------------------------------------
    // convertToDTO(Orders) — single-order path: must call the repository once
    // -------------------------------------------------------------------------

    @Test
    void convertToDTO_WithOrderOnly_LoadsItemsAndReturnsDTO() {
        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(List.of(orderItem1, orderItem2));

        OrderDTO result = orderMapper.convertToDTO(order);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(2, result.getItems().size());

        // Verify computed total
        BigDecimal expectedTotal = BigDecimal.valueOf(499.95).add(BigDecimal.valueOf(499.90));
        assertEquals(0, expectedTotal.compareTo(result.getTotalAmount()));

        OrderItemDTO first = result.getItems().get(0);
        assertEquals(itemId1,                  first.getItemId());
        assertEquals("Item 1",                 first.getItemName());
        assertEquals(5,                        first.getQuantity());
        assertEquals(BigDecimal.valueOf(99.99), first.getUnitPrice());
        assertEquals(BigDecimal.valueOf(499.95), first.getSubTotal());
    }

    @Test
    void convertToDTO_WithOrderAndNoItems_ReturnsDTOWithEmptyList() {
        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(List.of());

        OrderDTO result = orderMapper.convertToDTO(order);

        assertNotNull(result);
        assertEquals(0,            result.getItems().size());
        assertEquals(BigDecimal.ZERO, result.getTotalAmount());
    }

    @Test
    void convertToDTO_HandlesNullTimestamps() {
        order.setCreatedTimestamp(null);
        order.setUpdatedTimestamp(null);
        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(List.of());

        OrderDTO result = orderMapper.convertToDTO(order);

        assertNotNull(result);
        assertNull(result.getCreatedTimestamp());
        assertNull(result.getUpdatedTimestamp());
    }

    @Test
    void convertToDTO_PreservesOrderOfItems() {
        // Items returned in reverse order — mapping must preserve repository order
        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(List.of(orderItem2, orderItem1));

        OrderDTO result = orderMapper.convertToDTO(order);

        assertEquals(2,      result.getItems().size());
        assertEquals(itemId2, result.getItems().get(0).getItemId());
        assertEquals(itemId1, result.getItems().get(1).getItemId());
    }

    // -------------------------------------------------------------------------
    // convertToDTO(Orders, Map) — batch-list path: must NOT call the repository
    // -------------------------------------------------------------------------

    @Test
    void convertToDTO_WithItemsMap_ReturnsDTOWithoutCallingRepository() {
        Map<UUID, List<OrderItem>> itemsMap = Map.of(orderId, List.of(orderItem1, orderItem2));

        OrderDTO result = orderMapper.convertToDTO(order, itemsMap);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(2, result.getItems().size());

        BigDecimal expectedTotal = BigDecimal.valueOf(499.95).add(BigDecimal.valueOf(499.90));
        assertEquals(0, expectedTotal.compareTo(result.getTotalAmount()));

        // The repository must NOT have been called — no per-order DB query
        verify(orderItemRepository, never()).findByOrderOrderId(orderId);
    }

    @Test
    void convertToDTO_WithItemsMap_WhenOrderAbsentFromMap_ReturnsEmptyItemsAndZeroTotal() {
        // Order ID missing from map simulates an order that has no items yet
        Map<UUID, List<OrderItem>> emptyMap = Map.of();

        OrderDTO result = orderMapper.convertToDTO(order, emptyMap);

        assertNotNull(result);
        assertEquals(0,             result.getItems().size());
        assertEquals(BigDecimal.ZERO, result.getTotalAmount());

        verify(orderItemRepository, never()).findByOrderOrderId(orderId);
    }

    @Test
    void convertToDTO_WithItemsMap_PreservesItemOrder() {
        Map<UUID, List<OrderItem>> itemsMap = Map.of(orderId, List.of(orderItem2, orderItem1));

        OrderDTO result = orderMapper.convertToDTO(order, itemsMap);

        assertEquals(2,      result.getItems().size());
        assertEquals(itemId2, result.getItems().get(0).getItemId());
        assertEquals(itemId1, result.getItems().get(1).getItemId());
    }

    @Test
    void convertToDTO_WithItemsMap_SingleItem_ComputesCorrectSubtotal() {
        Map<UUID, List<OrderItem>> itemsMap = Map.of(orderId, List.of(orderItem1));

        OrderDTO result = orderMapper.convertToDTO(order, itemsMap);

        assertEquals(1, result.getItems().size());
        // 5 units × $99.99 = $499.95
        assertEquals(0, BigDecimal.valueOf(499.95).compareTo(result.getTotalAmount()));
    }

    // -------------------------------------------------------------------------
    // calculateTotal
    // -------------------------------------------------------------------------

    @Test
    void calculateTotal_WithItems_ReturnsCorrectSum() {
        BigDecimal result = orderMapper.calculateTotal(itemDTOs);
        assertEquals(BigDecimal.valueOf(999.85), result);
    }

    @Test
    void calculateTotal_WithEmptyList_ReturnsZero() {
        BigDecimal result = orderMapper.calculateTotal(List.of());
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateTotal_WithSingleItem_ReturnsItemSubTotal() {
        List<OrderItemDTO> single = List.of(
                new OrderItemDTO(itemId1, "Item 1", 5, BigDecimal.valueOf(99.99), BigDecimal.valueOf(499.95))
        );
        assertEquals(BigDecimal.valueOf(499.95), orderMapper.calculateTotal(single));
    }

    @Test
    void calculateTotal_WithNullSubTotal_ThrowsInvalidOperationException() {
        List<OrderItemDTO> itemsWithNull = List.of(
                new OrderItemDTO(itemId1, "Item 1", 5,  BigDecimal.valueOf(99.99), null),
                new OrderItemDTO(itemId2, "Item 2", 10, BigDecimal.valueOf(49.99), BigDecimal.valueOf(499.90))
        );
        assertThrows(InvalidOperationException.class, () -> orderMapper.calculateTotal(itemsWithNull));
    }
}
