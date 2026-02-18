package com.example.ordermgmt.service.impl.order;

import com.example.ordermgmt.dto.OrderItemDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.OrderItem;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.InsufficientStockException;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderInventoryManagerImpl {

    private static final Logger logger = LoggerFactory.getLogger(OrderInventoryManagerImpl.class);
    private final InventoryItemRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public List<OrderItemDTO> processAndSaveOrderItems(List<OrderItemDTO> items, Orders order) {
        return items.stream().map(itemReq -> {
            InventoryItem inventoryItem = inventoryRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> {
                        logger.error("Inventory item not found: {}", itemReq.getItemId());
                        return new InvalidOperationException("Item not found: " + itemReq.getItemId());
                    });

            if (inventoryItem.getPricingCatalog() == null) {
                logger.error("Pricing missing for item: {}", itemReq.getItemId());
                throw new InvalidOperationException("Price not found for item ID: " + itemReq.getItemId());
            }

            if (inventoryItem.getAvailableStock() < itemReq.getQuantity()) {
                logger.warn("Insufficient stock for item: {}. Requested: {}, Available: {}",
                        itemReq.getItemId(), itemReq.getQuantity(), inventoryItem.getAvailableStock());
                throw new InsufficientStockException("Insufficient stock for item: " + inventoryItem.getItemName()
                        + " (ID: " + inventoryItem.getItemId() + ")");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setId(new OrderItem.OrderItemId(order.getOrderId(), inventoryItem.getItemId()));
            orderItem.setOrder(order);
            orderItem.setInventoryItem(inventoryItem);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(inventoryItem.getPricingCatalog().getUnitPrice());

            orderItemRepository.save(orderItem);

            return new OrderItemDTO(
                    inventoryItem.getItemId(),
                    inventoryItem.getItemName(),
                    orderItem.getQuantity(),
                    orderItem.getUnitPrice(),
                    orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }).collect(Collectors.toList());
    }

    @Transactional
    public void handleInventoryUpdate(Orders order, OrderStatus currentStatus, OrderStatus nextStatus) {
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());

        if (nextStatus == OrderStatus.CONFIRMED && currentStatus == OrderStatus.PENDING) {
            for (OrderItem item : items) {
                InventoryItem inv = item.getInventoryItem();
                if (inv.getAvailableStock() < item.getQuantity()) {
                    logger.error("Stock check failed for item {} during confirmation", inv.getItemId());
                    throw new InsufficientStockException(
                            "Insufficient stock for item: " + inv.getItemName() + " (ID: " + inv.getItemId() + ")");
                }
                inv.setAvailableStock(inv.getAvailableStock() - item.getQuantity());
                inv.setReservedStock(inv.getReservedStock() + item.getQuantity());
                inventoryRepository.save(inv);
            }
        } else if (nextStatus == OrderStatus.CANCELLED &&
                (currentStatus == OrderStatus.CONFIRMED || currentStatus == OrderStatus.PROCESSING)) {
            for (OrderItem item : items) {
                InventoryItem inv = item.getInventoryItem();
                inv.setAvailableStock(inv.getAvailableStock() + item.getQuantity());
                inv.setReservedStock(inv.getReservedStock() - item.getQuantity());
                inventoryRepository.save(inv);
            }
        } else if (nextStatus == OrderStatus.SHIPPED && currentStatus == OrderStatus.PROCESSING) {
            for (OrderItem item : items) {
                InventoryItem inv = item.getInventoryItem();
                inv.setReservedStock(inv.getReservedStock() - item.getQuantity());
                inventoryRepository.save(inv);
            }
        }
    }
}
