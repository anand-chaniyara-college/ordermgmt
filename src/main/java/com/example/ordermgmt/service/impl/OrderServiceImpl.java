package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderItemDTO;
import com.example.ordermgmt.dto.OrderStatusUpdateDTO;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.entity.OrderItem;
import com.example.ordermgmt.entity.OrderStatusLookup;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.InsufficientStockException;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.InvalidOrderTransitionException;
import com.example.ordermgmt.exception.OrderNotFoundException;
import com.example.ordermgmt.repository.CustomerRepository;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.OrderItemRepository;
import com.example.ordermgmt.repository.OrdersRepository;
import com.example.ordermgmt.repository.OrderStatusLookupRepository;
import com.example.ordermgmt.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final InventoryItemRepository inventoryRepository;
    private final OrderStatusLookupRepository statusRepository;

    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO request, String email) {
        logger.info("Processing createOrder for Customer: {}", email);

        Customer customer = validateAndGetCustomer(email);
        validateCustomerProfile(customer);

        String orderId = generateOrderId();
        OrderStatusLookup pendingStatus = getStatusOrThrow(OrderStatus.PENDING.name());

        Orders order = new Orders();
        order.setOrderId(orderId);
        order.setCustomer(customer);
        order.setStatus(pendingStatus);

        ordersRepository.save(order);
        logger.info("Order entity saved with ID: {}", orderId);

        List<OrderItemDTO> itemDTOs = processAndSaveOrderItems(request.getItems(), order);

        BigDecimal total = calculateTotal(itemDTOs);

        logger.info("createOrder completed successfully for Customer: {}", email);
        return convertToDTO(order, itemDTOs, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getCustomerOrders(String email) {
        logger.info("Processing getCustomerOrders for Customer: {}", email);
        List<OrderDTO> orders = ordersRepository.findByCustomerAppUserEmail(email).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        logger.info("getCustomerOrders completed successfully for Customer: {}", email);
        return orders;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getCustomerOrderById(String orderId, String email) {
        logger.info("Processing getCustomerOrderById for Order: {}, Customer: {}", orderId, email);
        Orders order = getOrderOrThrow(orderId);

        if (!order.getCustomer().getAppUser().getEmail().equals(email)) {
            logger.warn("Access denied for user {} accessing order {}", email, orderId);
            throw new InvalidOperationException("Access denied");
        }

        logger.info("getCustomerOrderById completed successfully for Order: {}", orderId);
        return convertToDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(String orderId, String email) {
        logger.info("Processing cancelOrder for Order: {}, Customer: {}", orderId, email);
        Orders order = getOrderOrThrow(orderId);

        validateOrderOwnership(order, email);
        validateOrderCancellation(order);

        OrderStatusLookup cancelledStatus = getStatusOrThrow(OrderStatus.CANCELLED.name());
        handleInventoryUpdate(order, OrderStatus.valueOf(order.getStatus().getStatusName()), OrderStatus.CANCELLED);

        order.setStatus(cancelledStatus);
        ordersRepository.save(order);

        logger.info("cancelOrder completed successfully for Order: {}", orderId);
        return convertToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        logger.info("Processing getAllOrders for Admin");
        List<OrderDTO> orders = ordersRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        logger.info("getAllOrders completed successfully for Admin");
        return orders;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(String orderId) {
        logger.info("Processing getOrderById for Order: {}", orderId);
        Orders order = getOrderOrThrow(orderId);
        logger.info("getOrderById completed successfully for Order: {}", orderId);
        return convertToDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(String orderId, OrderStatusUpdateDTO statusUpdate) {
        String newStatusName = statusUpdate.getNewStatus().trim().toUpperCase();
        logger.info("Processing updateOrderStatus for Order: {} to {}", orderId, newStatusName);

        Orders order = getOrderOrThrow(orderId);
        OrderStatus currentStatus = OrderStatus.valueOf(order.getStatus().getStatusName());
        OrderStatus nextStatus = OrderStatus.valueOf(newStatusName);

        validateAdminTransition(currentStatus, nextStatus);

        OrderStatusLookup nextStatusLookup = getStatusOrThrow(newStatusName);

        handleInventoryUpdate(order, currentStatus, nextStatus);

        order.setStatus(nextStatusLookup);
        ordersRepository.save(order);

        logger.info("updateOrderStatus completed successfully for Order: {}", orderId);
        return convertToDTO(order);
    }

    private Customer validateAndGetCustomer(String email) {
        return customerRepository.findByAppUserEmail(email)
                .orElseThrow(() -> {
                    logger.error("Customer not found for email: {}", email);
                    return new InvalidOperationException("Customer not found");
                });
    }

    private void validateCustomerProfile(Customer customer) {
        if (isEmpty(customer.getFirstName()) || isEmpty(customer.getLastName()) || isEmpty(customer.getAddress())) {
            logger.warn("Customer profile incomplete for: {}", customer.getCustomerId());
            throw new InvalidOperationException("Customer profile incomplete");
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String generateOrderId() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderStatusLookup getStatusOrThrow(String statusName) {
        return statusRepository.findByStatusName(statusName)
                .orElseThrow(() -> {
                    logger.error("Status not found: {}", statusName);
                    return new InvalidOperationException("Status config missing: " + statusName);
                });
    }

    private Orders getOrderOrThrow(String orderId) {
        return ordersRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.warn("Order not found: {}", orderId);
                    return new OrderNotFoundException("Order not found: " + orderId);
                });
    }

    private List<OrderItemDTO> processAndSaveOrderItems(List<OrderItemDTO> items, Orders order) {
        return items.stream().map(itemReq -> {
            InventoryItem inventoryItem = inventoryRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> {
                        logger.error("Inventory item not found: {}", itemReq.getItemId());
                        return new InvalidOperationException("Item not found: " + itemReq.getItemId());
                    });

            if (inventoryItem.getPricingCatalog() == null) {
                logger.error("Pricing missing for item: {}", itemReq.getItemId());
                throw new InvalidOperationException("Price not found for: " + itemReq.getItemId());
            }

            if (inventoryItem.getAvailableStock() < itemReq.getQuantity()) {
                logger.warn("Insufficient stock for item: {}. Requested: {}, Available: {}",
                        itemReq.getItemId(), itemReq.getQuantity(), inventoryItem.getAvailableStock());
                throw new InsufficientStockException("Insufficient stock for: " + inventoryItem.getItemName());
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

    private BigDecimal calculateTotal(List<OrderItemDTO> items) {
        return items.stream()
                .map(OrderItemDTO::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateOrderOwnership(Orders order, String email) {
        if (order.getCustomer().getAppUser().getEmail() == null ||
                !order.getCustomer().getAppUser().getEmail().trim().equalsIgnoreCase(email.trim())) {
            logger.warn("Access denied! User [{}] is not owner of order [{}]", email, order.getOrderId());
            throw new InvalidOperationException("Access denied");
        }
    }

    private void validateOrderCancellation(Orders order) {
        String currentStatus = order.getStatus().getStatusName();
        if (!OrderStatus.PENDING.name().equalsIgnoreCase(currentStatus.trim())) {
            logger.warn("Attempt to cancel order {} with status {}", order.getOrderId(), currentStatus);
            throw new InvalidOrderTransitionException("Can only cancel PENDING orders");
        }
    }

    private void validateAdminTransition(OrderStatus current, OrderStatus next) {
        boolean valid = false;

        switch (current) {
            case PENDING:
                if (next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED)
                    valid = true;
                break;
            case CONFIRMED:
                if (next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED)
                    valid = true;
                break;
            case PROCESSING:
                if (next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED)
                    valid = true;
                break;
            case SHIPPED:
                if (next == OrderStatus.DELIVERED)
                    valid = true;
                break;
            default:
                break;
        }

        if (!valid) {
            logger.warn("Invalid status transition from {} to {}", current, next);
            throw new InvalidOrderTransitionException("Invalid transition from " + current + " to " + next);
        }
    }

    private void handleInventoryUpdate(Orders order, OrderStatus currentStatus, OrderStatus nextStatus) {
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());

        if (nextStatus == OrderStatus.CONFIRMED && currentStatus == OrderStatus.PENDING) {
            for (OrderItem item : items) {
                InventoryItem inv = item.getInventoryItem();
                if (inv.getAvailableStock() < item.getQuantity()) {
                    logger.error("Stock check failed for item {} during confirmation", inv.getItemId());
                    throw new InsufficientStockException("Insufficient stock for: " + inv.getItemId());
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

    private OrderDTO convertToDTO(Orders order) {
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
        List<OrderItemDTO> itemDTOs = items.stream().map(item -> new OrderItemDTO(
                item.getInventoryItem().getItemId(),
                item.getInventoryItem().getItemName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))).collect(Collectors.toList());
        BigDecimal total = calculateTotal(itemDTOs);
        return convertToDTO(order, itemDTOs, total);
    }

    private OrderDTO convertToDTO(Orders order, List<OrderItemDTO> items, BigDecimal total) {
        return new OrderDTO(
                order.getOrderId(),
                order.getCustomer().getCustomerId(),
                order.getStatus().getStatusName(),
                order.getCreatedTimestamp(),
                order.getUpdatedTimestamp(),
                items,
                total);
    }
}
