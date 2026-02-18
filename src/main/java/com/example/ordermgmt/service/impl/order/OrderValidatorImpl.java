package com.example.ordermgmt.service.impl.order;

import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.entity.OrderStatusLookup;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.InvalidOrderTransitionException;
import com.example.ordermgmt.repository.CustomerRepository;
import com.example.ordermgmt.repository.OrderStatusLookupRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderValidatorImpl {

    private static final Logger logger = LoggerFactory.getLogger(OrderValidatorImpl.class);
    private final CustomerRepository customerRepository;
    private final OrderStatusLookupRepository statusRepository;

    public Customer validateAndGetCustomer(String email) {
        return customerRepository.findByAppUserEmail(email)
                .orElseThrow(() -> {
                    logger.error("Customer not found for email: {}", email);
                    return new InvalidOperationException("Customer not found for email: " + email);
                });
    }

    public void validateCustomerProfile(Customer customer) {
        if (isEmpty(customer.getFirstName()) || isEmpty(customer.getLastName()) || isEmpty(customer.getAddress())) {
            logger.warn("Customer profile incomplete for: {}", customer.getCustomerId());
            throw new InvalidOperationException("Customer profile incomplete for ID: " + customer.getCustomerId());
        }
    }

    public void validateOrderOwnership(Orders order, String email) {
        if (order.getCustomer().getAppUser().getEmail() == null ||
                !order.getCustomer().getAppUser().getEmail().trim().equalsIgnoreCase(email.trim())) {
            logger.warn("Access denied! User [{}] is not owner of order [{}]", email, order.getOrderId());
            throw new InvalidOperationException(
                    "Access denied for user: " + email + " on order: " + order.getOrderId());
        }
    }

    public void validateOrderCancellation(Orders order) {
        String currentStatus = order.getStatus().getStatusName();
        if (!OrderStatus.PENDING.name().equalsIgnoreCase(currentStatus.trim())) {
            logger.warn("Attempt to cancel order {} with status {}", order.getOrderId(), currentStatus);
            throw new InvalidOrderTransitionException(
                    "Cannot cancel order " + order.getOrderId() + ". Current status: " + currentStatus);
        }
    }

    public void validateAdminTransition(OrderStatus current, OrderStatus next) {
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

    public OrderStatusLookup getStatusOrThrow(String statusName) {
        return statusRepository.findByStatusName(statusName)
                .orElseThrow(() -> {
                    logger.error("Status not found: {}", statusName);
                    return new InvalidOperationException("Status config missing: " + statusName);
                });
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
