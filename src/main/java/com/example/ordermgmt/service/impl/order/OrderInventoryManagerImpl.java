package com.example.ordermgmt.service.impl.order;

import com.example.ordermgmt.dto.OrderItemDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.OrderItem;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.entity.PricingHistory;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.InsufficientStockException;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.OrderItemRepository;
import com.example.ordermgmt.repository.PricingHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderInventoryManagerImpl {

        private static final Logger logger = LoggerFactory.getLogger(OrderInventoryManagerImpl.class);

        private final InventoryItemRepository inventoryRepository;
        private final OrderItemRepository orderItemRepository;
        private final PricingHistoryRepository pricingHistoryRepository;

        /**
         * Process order items with pessimistic locking and price-from-history.
         * Items are sorted by itemId before lock acquisition to prevent deadlocks.
         */
        @Transactional
        public List<OrderItemDTO> processAndSaveOrderItems(List<OrderItemDTO> items, Orders order) {
                logger.info("Processing processAndSaveOrderItems for Order: {}", order.getOrderId());
                List<OrderItemDTO> sortedItems = sortByItemId(items);
                Map<UUID, InventoryItem> lockedInventory = acquireInventoryLocks(sortedItems);

                List<OrderItemDTO> result = sortedItems.stream()
                                .map(itemReq -> processItem(itemReq, lockedInventory, order))
                                .collect(Collectors.toList());

                logger.info("processAndSaveOrderItems completed successfully for Order: {}", order.getOrderId());
                return result;
        }

        private List<OrderItemDTO> sortByItemId(List<OrderItemDTO> items) {
                return items.stream()
                                .sorted(Comparator.comparing(OrderItemDTO::getItemId))
                                .collect(Collectors.toList());
        }

        private Map<UUID, InventoryItem> acquireInventoryLocks(List<OrderItemDTO> items) {
                List<UUID> itemIds = items.stream()
                                .map(OrderItemDTO::getItemId)
                                .collect(Collectors.toList());
                return inventoryRepository
                                .findAllByItemIdInForUpdate(itemIds).stream()
                                .collect(Collectors.toMap(InventoryItem::getItemId, Function.identity()));
        }

        private OrderItemDTO processItem(OrderItemDTO itemReq, Map<UUID, InventoryItem> lockedInventory, Orders order) {
                InventoryItem inventoryItem = lockedInventory.get(itemReq.getItemId());
                if (inventoryItem == null) {
                        throw new InvalidOperationException("Item not found: " + itemReq.getItemId());
                }

                int availableStock = inventoryItem.getAvailableStock();
                if (availableStock < itemReq.getQuantity()) {
                        throw new InsufficientStockException("Insufficient stock for item: "
                                        + inventoryItem.getItemName() + " (ID: " + inventoryItem.getItemId()
                                        + "). Available: " + availableStock + ", Requested: "
                                        + itemReq.getQuantity());
                }

                BigDecimal unitPrice = resolveUnitPrice(inventoryItem);

                inventoryItem.setAvailableStock(inventoryItem.getAvailableStock() - itemReq.getQuantity());
                inventoryItem.setReservedStock(inventoryItem.getReservedStock() + itemReq.getQuantity());
                inventoryRepository.saveAndFlush(inventoryItem);
                logger.debug("PENDING: Item {} reserved — available: {}, reserved: {}",
                                inventoryItem.getItemId(), inventoryItem.getAvailableStock(),
                                inventoryItem.getReservedStock());

                // Create and save OrderItem
                OrderItem orderItem = new OrderItem();
                orderItem.setId(new OrderItem.OrderItemId(order.getOrderId(), inventoryItem.getItemId()));
                orderItem.setOrder(order);
                orderItem.setInventoryItem(inventoryItem);
                orderItem.setQuantity(itemReq.getQuantity());
                orderItem.setUnitPrice(unitPrice);
                orderItemRepository.save(orderItem);

                return new OrderItemDTO(
                                inventoryItem.getItemId(),
                                inventoryItem.getItemName(),
                                orderItem.getQuantity(),
                                orderItem.getUnitPrice(),
                                orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }

        /**
         * Handle inventory updates based on specific order status transitions.
         * Uses pessimistic locking with deterministic lock order to prevent deadlocks.
         *
         * Inventory transitions handled:
         * 1. Revert Reservation: (Any Status) -> CANCELLED (availableStock += qty, reservedStock -= qty)
         * 2. Final Fulfillment: SHIPPED -> DELIVERED (reservedStock -= qty)
         * 3. No-op: All other transitions (no inventory impact)
         */
        @Transactional
        public void handleInventoryUpdate(Orders order, OrderStatus currentStatus, OrderStatus nextStatus) {
                logger.info("Processing handleInventoryUpdate for Order: {} ({} -> {})",
                                order.getOrderId(), currentStatus, nextStatus);

                List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());

                if (items.isEmpty()) {
                        logger.warn("Skipping handleInventoryUpdate for Order: {} - no order items found",
                                        order.getOrderId());
                        return;
                }

                // Collect sorted itemIds for deterministic lock order
                List<UUID> itemIds = items.stream()
                                .map(i -> i.getInventoryItem().getItemId())
                                .sorted()
                                .distinct()
                                .collect(Collectors.toList());
                Map<UUID, InventoryItem> locked = inventoryRepository
                                .findAllByItemIdInForUpdate(itemIds).stream()
                                .collect(Collectors.toMap(InventoryItem::getItemId, Function.identity()));

                if (nextStatus == OrderStatus.CANCELLED) {
                        // Revert reservation: available ++, reserved --
                        for (OrderItem item : items) {
                                InventoryItem inv = locked.get(item.getInventoryItem().getItemId());
                                releaseReservedStock(inv, item.getQuantity());
                                inv.setAvailableStock(inv.getAvailableStock() + item.getQuantity());
                                inventoryRepository.save(inv);
                                logger.debug("CANCELLED: Item {} reservation reverted — available: {}, reserved: {}",
                                                inv.getItemId(), inv.getAvailableStock(), inv.getReservedStock());
                        }
                } else if (nextStatus == OrderStatus.DELIVERED && currentStatus == OrderStatus.SHIPPED) {
                        // Final fulfillment: reserved --
                        for (OrderItem item : items) {
                                InventoryItem inv = locked.get(item.getInventoryItem().getItemId());
                                releaseReservedStock(inv, item.getQuantity());
                                inventoryRepository.save(inv);
                                logger.debug("DELIVERED: Item {} fulfilled — available: {}, reserved: {}",
                                                inv.getItemId(), inv.getAvailableStock(), inv.getReservedStock());
                        }
                }
        }

        /**
         * Resolve unit price: prefer PricingHistory (immutable) over PricingCatalog
         * (mutable).
         */
        private BigDecimal resolveUnitPrice(InventoryItem inventoryItem) {
                // 1. Try PricingHistory first (immutable snapshot)
                Optional<PricingHistory> latestHistory = pricingHistoryRepository
                                .findFirstByInventoryItemItemIdOrderByCreatedTimestampDesc(inventoryItem.getItemId());

                if (latestHistory.isPresent()) {
                        logger.debug("Price for item {} resolved from PricingHistory: {}",
                                        inventoryItem.getItemId(), latestHistory.get().getNewPrice());
                        return latestHistory.get().getNewPrice();
                }

                // 2. Fallback to PricingCatalog (backward compatibility)
                if (inventoryItem.getPricingCatalog() != null
                                && inventoryItem.getPricingCatalog().getUnitPrice() != null) {
                        logger.warn("Skipping PricingHistory for Item: {} - no history found, falling back to PricingCatalog: {}",
                                        inventoryItem.getItemId(), inventoryItem.getPricingCatalog().getUnitPrice());
                        return inventoryItem.getPricingCatalog().getUnitPrice();
                }

                throw new InvalidOperationException(
                                "Price not found for item ID: " + inventoryItem.getItemId()
                                                + ". Ensure pricing is configured before accepting orders.");
        }

        private void releaseReservedStock(InventoryItem inventoryItem, int quantity) {
                int currentReserved = inventoryItem.getReservedStock();
                int newReserved = currentReserved - quantity;
                if (newReserved < 0) {
                        throw new InvalidOperationException(
                                        "Stock inconsistency for item " + inventoryItem.getItemId()
                                                        + ": cannot release " + quantity
                                                        + " units, only " + currentReserved + " reserved");
                }
                inventoryItem.setReservedStock(newReserved);
        }
}
