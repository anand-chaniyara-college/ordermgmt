package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderItemDTO;
import com.example.ordermgmt.dto.OrderStatusUpdateDTO;
import com.example.ordermgmt.entity.*;
import com.example.ordermgmt.repository.*;
import com.example.ordermgmt.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final PricingCatalogRepository pricingRepository;
    private final OrderStatusLookupRepository statusRepository;

    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO request, String email) {
        logger.info("Creating order for customer: {}", email);
        Customer customer = customerRepository.findByAppUserEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        if (customer.getFirstName() == null || customer.getFirstName().trim().isEmpty() ||
                customer.getLastName() == null || customer.getLastName().trim().isEmpty() ||
                customer.getAddress() == null || customer.getAddress().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer profile incomplete");
        }

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OrderStatusLookup pendingStatus = statusRepository.findByStatusName("PENDING")
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PENDING status missing"));

        Orders order = new Orders();
        order.setOrderId(orderId);
        order.setCustomer(customer);
        order.setStatus(pendingStatus);
        order.setCreatedTimestamp(LocalDateTime.now());
        order.setUpdatedTimestamp(LocalDateTime.now());

        ordersRepository.save(order);

        List<OrderItemDTO> itemDTOs = request.getItems().stream().map(itemReq -> {
            InventoryItem inventoryItem = inventoryRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Item not found: " + itemReq.getItemId()));

            if (itemReq.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive");
            }

            PricingCatalog pricing = pricingRepository
                    .findFirstByInventoryItemItemIdOrderByCreatedTimestampDesc(itemReq.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Price not found for: " + itemReq.getItemId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setId(new OrderItem.OrderItemId(orderId, inventoryItem.getItemId()));
            orderItem.setOrder(order);
            orderItem.setInventoryItem(inventoryItem);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(pricing.getUnitPrice());

            orderItemRepository.save(orderItem);

            return new OrderItemDTO(
                    inventoryItem.getItemId(),
                    inventoryItem.getItemName(),
                    orderItem.getQuantity(),
                    orderItem.getUnitPrice(),
                    orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }).collect(Collectors.toList());

        BigDecimal total = itemDTOs.stream()
                .map(OrderItemDTO::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return convertToDTO(order, itemDTOs, total);
    }

    @Override
    public List<OrderDTO> getCustomerOrders(String email) {
        return ordersRepository.findByCustomerAppUserEmail(email).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDTO getCustomerOrderById(String orderId, String email) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getCustomer().getAppUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return convertToDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(String orderId, String email) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getCustomer().getAppUser().getEmail() == null ||
                !order.getCustomer().getAppUser().getEmail().trim().equalsIgnoreCase(email.trim())) {
            logger.warn("Access denied! User [{}] is not owner of order [{}]", email, orderId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        String currentStatus = order.getStatus().getStatusName();
        if (!"PENDING".equalsIgnoreCase(currentStatus.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only cancel PENDING orders");
        }

        OrderStatusLookup cancelledStatus = statusRepository.findByStatusName("CANCELLED")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "CANCELLED status missing"));

        order.setStatus(cancelledStatus);
        order.setUpdatedTimestamp(LocalDateTime.now());
        ordersRepository.save(order);

        return convertToDTO(order);
    }

    @Override
    public List<OrderDTO> getAllOrders() {
        return ordersRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDTO getOrderById(String orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return convertToDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(String orderId, OrderStatusUpdateDTO statusUpdate) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        String currentStatus = order.getStatus().getStatusName().trim().toUpperCase();
        String newStatusName = statusUpdate.getNewStatus().trim().toUpperCase();

        validateAdminTransition(currentStatus, newStatusName);

        OrderStatusLookup nextStatus = statusRepository.findByStatusName(newStatusName)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + newStatusName));

        order.setStatus(nextStatus);
        order.setUpdatedTimestamp(LocalDateTime.now());
        ordersRepository.save(order);

        handleInventoryUpdate(order, currentStatus, newStatusName);

        return convertToDTO(order);
    }

    private void validateAdminTransition(String current, String next) {
        boolean valid = false;
        switch (current) {
            case "PENDING":
                if ("CONFIRMED".equals(next) || "CANCELLED".equals(next))
                    valid = true;
                break;
            case "CONFIRMED":
                if ("PROCESSING".equals(next) || "CANCELLED".equals(next))
                    valid = true;
                break;
            case "PROCESSING":
                if ("SHIPPED".equals(next) || "CANCELLED".equals(next))
                    valid = true;
                break;
            case "SHIPPED":
                if ("DELIVERED".equals(next))
                    valid = true;
                break;
        }
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid transition from " + current + " to " + next);
        }
    }

    private void handleInventoryUpdate(Orders order, String currentStatus, String nextStatus) {
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
        if ("CONFIRMED".equals(nextStatus) && "PENDING".equals(currentStatus)) {
            for (OrderItem item : items) {
                InventoryItem inv = item.getInventoryItem();
                if (inv.getAvailableStock() < item.getQuantity()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Insufficient stock for: " + inv.getItemId());
                }
                inv.setAvailableStock(inv.getAvailableStock() - item.getQuantity());
                inv.setReservedStock(inv.getReservedStock() + item.getQuantity());
                inventoryRepository.save(inv);
            }
        } else if ("CANCELLED".equals(nextStatus)
                && ("CONFIRMED".equals(currentStatus) || "PROCESSING".equals(currentStatus))) {
            for (OrderItem item : items) {
                InventoryItem inv = item.getInventoryItem();
                inv.setAvailableStock(inv.getAvailableStock() + item.getQuantity());
                inv.setReservedStock(inv.getReservedStock() - item.getQuantity());
                inventoryRepository.save(inv);
            }
        } else if ("SHIPPED".equals(nextStatus) && "PROCESSING".equals(currentStatus)) {
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

        BigDecimal total = itemDTOs.stream()
                .map(OrderItemDTO::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
