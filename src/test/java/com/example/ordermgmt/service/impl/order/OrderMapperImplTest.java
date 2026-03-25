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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        itemId1 = UUID.randomUUID();
        itemId2 = UUID.randomUUID();

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
                new OrderItemDTO(itemId1, "Item 1", 5, BigDecimal.valueOf(99.99), BigDecimal.valueOf(499.95)),
                new OrderItemDTO(itemId2, "Item 2", 10, BigDecimal.valueOf(49.99), BigDecimal.valueOf(499.90))
        );
    }

    @Test
    void convertToDTO_WithItemsAndTotal_ReturnsCompleteDTO() {
        BigDecimal total = BigDecimal.valueOf(999.85);

        OrderDTO result = orderMapper.convertToDTO(order, itemDTOs, total);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(customerId, result.getCustomerId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(order.getCreatedTimestamp(), result.getCreatedTimestamp());
        assertEquals(order.getUpdatedTimestamp(), result.getUpdatedTimestamp());
        assertEquals(2, result.getItems().size());
        assertEquals(total, result.getTotalAmount());
    }

    @Test
    void convertToDTO_WithOrderOnly_LoadsItemsAndReturnsDTO() {
        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(List.of(orderItem1, orderItem2));

        OrderDTO result = orderMapper.convertToDTO(order);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(2, result.getItems().size());
        
        // Verify calculated total
        BigDecimal expectedTotal = BigDecimal.valueOf(499.95).add(BigDecimal.valueOf(499.90));
        assertEquals(0, expectedTotal.compareTo(result.getTotalAmount()));
        
        // Verify item details
        OrderItemDTO firstItem = result.getItems().get(0);
        assertEquals(itemId1, firstItem.getItemId());
        assertEquals("Item 1", firstItem.getItemName());
        assertEquals(5, firstItem.getQuantity());
        assertEquals(BigDecimal.valueOf(99.99), firstItem.getUnitPrice());
        assertEquals(BigDecimal.valueOf(499.95), firstItem.getSubTotal());
    }

    @Test
    void convertToDTO_WithOrderAndNoItems_ReturnsDTOWithEmptyList() {
        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(List.of());

        OrderDTO result = orderMapper.convertToDTO(order);

        assertNotNull(result);
        assertEquals(0, result.getItems().size());
        assertEquals(BigDecimal.ZERO, result.getTotalAmount());
    }

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
        List<OrderItemDTO> singleItem = List.of(
                new OrderItemDTO(itemId1, "Item 1", 5, BigDecimal.valueOf(99.99), BigDecimal.valueOf(499.95))
        );

        BigDecimal result = orderMapper.calculateTotal(singleItem);

        assertEquals(BigDecimal.valueOf(499.95), result);
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
        // Create items in different order
        List<OrderItem> unorderedItems = List.of(orderItem2, orderItem1);
        when(orderItemRepository.findByOrderOrderId(orderId)).thenReturn(unorderedItems);

        OrderDTO result = orderMapper.convertToDTO(order);

        // Should preserve the order from repository
        assertEquals(2, result.getItems().size());
        assertEquals(itemId2, result.getItems().get(0).getItemId()); // orderItem2 first
        assertEquals(itemId1, result.getItems().get(1).getItemId()); // orderItem1 second
    }

    @Test
    void calculateTotal_WithNullSubTotal_HandlesGracefully() {
        List<OrderItemDTO> itemsWithNull = List.of(
                new OrderItemDTO(itemId1, "Item 1", 5, BigDecimal.valueOf(99.99), null),
                new OrderItemDTO(itemId2, "Item 2", 10, BigDecimal.valueOf(49.99), BigDecimal.valueOf(499.90))
        );

        assertThrows(InvalidOperationException.class, () -> orderMapper.calculateTotal(itemsWithNull));
    }
}
