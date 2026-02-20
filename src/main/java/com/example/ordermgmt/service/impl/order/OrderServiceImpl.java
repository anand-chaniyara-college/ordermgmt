package com.example.ordermgmt.service.impl.order;

import com.example.ordermgmt.dto.BulkOrderFailureDTO;
import com.example.ordermgmt.dto.BulkOrderStatusUpdateDTO;
import com.example.ordermgmt.dto.BulkOrderUpdateResultDTO;
import com.example.ordermgmt.dto.OrderDTO;
import com.example.ordermgmt.dto.OrderItemDTO;
import com.example.ordermgmt.dto.OrderStatusUpdateDTO;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.entity.OrderStatusLookup;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.InsufficientStockException;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.OrderNotFoundException;
import com.example.ordermgmt.repository.OrdersRepository;
import com.example.ordermgmt.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final String ORDER_ID_PREFIX = "ORD-";
    private static final int ORDER_ID_SUFFIX_LENGTH = 8;
    private static final int START_INDEX = 0;
    private static final int MAX_ORDER_ID_RETRIES = 5;

    private final OrdersRepository ordersRepository;
    private final OrderValidatorImpl orderValidator;
    private final OrderInventoryManagerImpl orderInventoryManager;
    private final OrderMapperImpl orderMapper;
    private final OrderTransitionHelper transitionHelper;

    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO request, String email) {
        logger.info("Processing createOrder for Customer: {}", email);

        Customer customer = orderValidator.validateAndGetCustomer(email);
        orderValidator.validateCustomerProfile(customer);

        String orderId = generateUniqueOrderId();
        OrderStatusLookup pendingStatus = orderValidator.getStatusOrThrow(OrderStatus.PENDING.name());

        Orders order = new Orders();
        order.setOrderId(orderId);
        order.setCustomer(customer);
        order.setStatus(pendingStatus);

        ordersRepository.saveAndFlush(order);
        logger.info("Order entity saved with ID: {}", orderId);

        List<OrderItemDTO> itemDTOs = orderInventoryManager.processAndSaveOrderItems(request.getItems(), order);

        BigDecimal total = orderMapper.calculateTotal(itemDTOs);

        logger.info("createOrder completed successfully for Customer: {}", email);
        return orderMapper.convertToDTO(order, itemDTOs, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getCustomerOrders(String email) {
        logger.info("Processing getCustomerOrders for Customer: {}", email);
        List<OrderDTO> orders = ordersRepository.findByCustomerAppUserEmail(email).stream()
                .map(orderMapper::convertToDTO)
                .collect(Collectors.toList());
        logger.info("getCustomerOrders completed successfully for Customer: {}", email);
        return orders;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getCustomerOrders(String email, Pageable pageable) {
        logger.info("Processing getCustomerOrders (Page) for Customer: {}", email);
        Page<OrderDTO> orders = ordersRepository.findByCustomerAppUserEmail(email, pageable)
                .map(orderMapper::convertToDTO);
        logger.info("getCustomerOrders (Page) completed successfully for Customer: {}", email);
        return orders;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getCustomerOrderById(String orderId, String email) {
        logger.info("Processing getCustomerOrderById for Order: {}, Customer: {}", orderId, email);
        Orders order = getOrderOrThrow(orderId);

        orderValidator.validateOrderOwnership(order, email);

        logger.info("getCustomerOrderById completed successfully for Order: {}", orderId);
        return orderMapper.convertToDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(String orderId, String email) {
        logger.info("Processing cancelOrder for Order: {}, Customer: {}", orderId, email);
        Orders order = getOrderOrThrow(orderId);

        orderValidator.validateOrderOwnership(order, email);
        orderValidator.validateOrderCancellation(order);

        OrderStatusLookup cancelledStatus = orderValidator.getStatusOrThrow(OrderStatus.CANCELLED.name());
        orderInventoryManager.handleInventoryUpdate(order, OrderStatus.valueOf(order.getStatus().getStatusName()),
                OrderStatus.CANCELLED);

        order.setStatus(cancelledStatus);
        ordersRepository.save(order);

        logger.info("cancelOrder completed successfully for Order: {}", orderId);
        return orderMapper.convertToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        logger.info("Processing getAllOrders for Admin");
        List<OrderDTO> orders = ordersRepository.findAll().stream()
                .map(orderMapper::convertToDTO)
                .collect(Collectors.toList());
        logger.info("getAllOrders completed successfully for Admin");
        return orders;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        logger.info("Processing getAllOrders (Page) for Admin");
        Page<OrderDTO> orders = ordersRepository.findAll(pageable)
                .map(orderMapper::convertToDTO);
        logger.info("getAllOrders (Page) completed successfully for Admin");
        return orders;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(String orderId) {
        logger.info("Processing getOrderById for Order: {}", orderId);
        Orders order = getOrderOrThrow(orderId);
        logger.info("getOrderById completed successfully for Order: {}", orderId);
        return orderMapper.convertToDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(String orderId, OrderStatusUpdateDTO statusUpdate) {
        logger.info("Processing updateOrderStatus for Order: {}", orderId);
        return transitionHelper.updateOrderInternal(orderId, statusUpdate.getNewStatus());
    }

    @Override
    public BulkOrderUpdateResultDTO updateOrdersStatus(List<BulkOrderStatusUpdateDTO> updates) {
        logger.info("Processing updateOrdersStatus for {} orders", updates.size());
        List<OrderDTO> successes = new ArrayList<>();
        List<BulkOrderFailureDTO> failures = new ArrayList<>();

        for (BulkOrderStatusUpdateDTO update : updates) {
            try {
                OrderDTO result = transitionHelper.updateOrderInternal(
                        update.getOrderId(), update.getNewStatus());
                successes.add(result);
            } catch (OrderNotFoundException | InvalidOperationException | InsufficientStockException e) {
                logger.error("Bulk update failed for Order: {}: {}", update.getOrderId(), e.getMessage());
                failures.add(new BulkOrderFailureDTO(update.getOrderId(), e.getMessage()));
            }
        }

        logger.info("updateOrdersStatus completed: {} successes, {} failures",
                successes.size(), failures.size());
        return new BulkOrderUpdateResultDTO(successes, failures);
    }

    /**
     * Generate unique order ID with collision retry (max 5 attempts).
     */
    private String generateUniqueOrderId() {
        for (int i = 0; i < MAX_ORDER_ID_RETRIES; i++) {
            String orderId = ORDER_ID_PREFIX + UUID.randomUUID().toString()
                    .substring(START_INDEX, ORDER_ID_SUFFIX_LENGTH).toUpperCase();
            if (!ordersRepository.existsById(orderId)) {
                return orderId;
            }
            logger.warn("Order ID collision detected: {}. Retrying ({}/{})",
                    orderId, i + 1, MAX_ORDER_ID_RETRIES);
        }
        throw new InvalidOperationException(
                "Failed to generate unique order ID after " + MAX_ORDER_ID_RETRIES + " attempts");
    }

    private Orders getOrderOrThrow(String orderId) {
        return ordersRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.warn("Order not found: {}", orderId);
                    return new OrderNotFoundException("Order not found: " + orderId);
                });
    }
}
