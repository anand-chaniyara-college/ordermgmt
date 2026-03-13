package com.example.ordermgmt.service.impl.order;

import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.Orders;
import com.example.ordermgmt.entity.OrderStatusLookup;
import com.example.ordermgmt.enums.OrderStatus;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.InvalidOrderTransitionException;
import com.example.ordermgmt.repository.CustomerRepository;
import com.example.ordermgmt.repository.OrderStatusLookupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderValidatorImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderStatusLookupRepository statusRepository;

    @InjectMocks
    private OrderValidatorImpl orderValidator;

    private UUID customerId;
    private UUID orderId;
    private String email;
    private Customer customer;
    private AppUser appUser;
    private Orders order;
    private OrderStatusLookup statusLookup;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        email = "test@example.com";

        appUser = new AppUser();
        appUser.setEmail(email);

        customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setAddress("123 Test St");
        customer.setAppUser(appUser);

        statusLookup = new OrderStatusLookup();
        statusLookup.setStatusName("PENDING");

        order = new Orders();
        order.setOrderId(orderId);
        order.setCustomer(customer);
        order.setStatus(statusLookup);
    }

    @Test
    void validateAndGetCustomer_WithExistingEmail_ReturnsCustomer() {
        when(customerRepository.findByAppUserEmail(email)).thenReturn(Optional.of(customer));

        Customer result = orderValidator.validateAndGetCustomer(email);

        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
    }

    @Test
    void validateAndGetCustomer_WithNonExistingEmail_ThrowsException() {
        when(customerRepository.findByAppUserEmail(email)).thenReturn(Optional.empty());

        assertThrows(InvalidOperationException.class, () ->
                orderValidator.validateAndGetCustomer(email));
    }

    @Test
    void validateCustomerProfile_WithCompleteProfile_DoesNothing() {
        assertDoesNotThrow(() -> orderValidator.validateCustomerProfile(customer));
    }

    @Test
    void validateCustomerProfile_WithMissingFirstName_ThrowsException() {
        customer.setFirstName(null);
        assertThrows(InvalidOperationException.class, () ->
                orderValidator.validateCustomerProfile(customer));
    }

    @Test
    void validateCustomerProfile_WithEmptyFirstName_ThrowsException() {
        customer.setFirstName("");
        assertThrows(InvalidOperationException.class, () ->
                orderValidator.validateCustomerProfile(customer));
    }

    @Test
    void validateCustomerProfile_WithMissingLastName_ThrowsException() {
        customer.setLastName(null);
        assertThrows(InvalidOperationException.class, () ->
                orderValidator.validateCustomerProfile(customer));
    }

    @Test
    void validateCustomerProfile_WithMissingAddress_ThrowsException() {
        customer.setAddress(null);
        assertThrows(InvalidOperationException.class, () ->
                orderValidator.validateCustomerProfile(customer));
    }

    @Test
    void validateOrderOwnership_WithCorrectOwner_DoesNothing() {
        assertDoesNotThrow(() -> orderValidator.validateOrderOwnership(order, email));
    }

    @Test
    void validateOrderOwnership_WithWrongOwner_ThrowsException() {
        String wrongEmail = "wrong@example.com";
        assertThrows(InvalidOperationException.class, () ->
                orderValidator.validateOrderOwnership(order, wrongEmail));
    }

    @Test
    void validateOrderOwnership_WithCaseInsensitiveEmail_DoesNothing() {
        assertDoesNotThrow(() -> orderValidator.validateOrderOwnership(order, "TEST@EXAMPLE.COM"));
    }

    @Test
    void validateOrderCancellation_WithPendingOrder_DoesNothing() {
        assertDoesNotThrow(() -> orderValidator.validateOrderCancellation(order));
    }

    @Test
    void validateOrderCancellation_WithNonPendingOrder_ThrowsException() {
        statusLookup.setStatusName("CONFIRMED");
        assertThrows(InvalidOrderTransitionException.class, () ->
                orderValidator.validateOrderCancellation(order));
    }

    @Test
    void validateAdminTransition_PendingToConfirmed_Valid() {
        assertDoesNotThrow(() -> 
                orderValidator.validateAdminTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED));
    }

    @Test
    void validateAdminTransition_PendingToCancelled_Valid() {
        assertDoesNotThrow(() -> 
                orderValidator.validateAdminTransition(OrderStatus.PENDING, OrderStatus.CANCELLED));
    }

    @Test
    void validateAdminTransition_PendingToProcessing_Invalid() {
        assertThrows(InvalidOrderTransitionException.class, () ->
                orderValidator.validateAdminTransition(OrderStatus.PENDING, OrderStatus.PROCESSING));
    }

    @Test
    void validateAdminTransition_ConfirmedToProcessing_Valid() {
        assertDoesNotThrow(() -> 
                orderValidator.validateAdminTransition(OrderStatus.CONFIRMED, OrderStatus.PROCESSING));
    }

    @Test
    void validateAdminTransition_ConfirmedToCancelled_Valid() {
        assertDoesNotThrow(() -> 
                orderValidator.validateAdminTransition(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
    }

    @Test
    void validateAdminTransition_ConfirmedToShipped_Invalid() {
        assertThrows(InvalidOrderTransitionException.class, () ->
                orderValidator.validateAdminTransition(OrderStatus.CONFIRMED, OrderStatus.SHIPPED));
    }

    @Test
    void validateAdminTransition_ProcessingToShipped_Valid() {
        assertDoesNotThrow(() -> 
                orderValidator.validateAdminTransition(OrderStatus.PROCESSING, OrderStatus.SHIPPED));
    }

    @Test
    void validateAdminTransition_ProcessingToCancelled_Valid() {
        assertDoesNotThrow(() -> 
                orderValidator.validateAdminTransition(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
    }

    @Test
    void validateAdminTransition_ProcessingToDelivered_Invalid() {
        assertThrows(InvalidOrderTransitionException.class, () ->
                orderValidator.validateAdminTransition(OrderStatus.PROCESSING, OrderStatus.DELIVERED));
    }

    @Test
    void validateAdminTransition_ShippedToDelivered_Valid() {
        assertDoesNotThrow(() -> 
                orderValidator.validateAdminTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED));
    }

    @Test
    void validateAdminTransition_ShippedToCancelled_Invalid() {
        assertThrows(InvalidOrderTransitionException.class, () ->
                orderValidator.validateAdminTransition(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
    }

    @Test
    void validateAdminTransition_DeliveredToAny_Invalid() {
        assertThrows(InvalidOrderTransitionException.class, () ->
                orderValidator.validateAdminTransition(OrderStatus.DELIVERED, OrderStatus.CANCELLED));
    }

    @Test
    void getStatusOrThrow_WithExistingStatus_ReturnsStatus() {
        when(statusRepository.findByStatusName("PENDING")).thenReturn(Optional.of(statusLookup));

        OrderStatusLookup result = orderValidator.getStatusOrThrow("PENDING");

        assertNotNull(result);
        assertEquals("PENDING", result.getStatusName());
    }

    @Test
    void getStatusOrThrow_WithNonExistingStatus_ThrowsException() {
        when(statusRepository.findByStatusName("INVALID")).thenReturn(Optional.empty());

        assertThrows(InvalidOperationException.class, () ->
                orderValidator.getStatusOrThrow("INVALID"));
    }

    @Test
    void validateOrderOwnership_WithNullEmail_ThrowsException() {
        appUser.setEmail(null);
        assertThrows(InvalidOperationException.class, () ->
                orderValidator.validateOrderOwnership(order, email));
    }

    @Test
    void validateCustomerProfile_WithWhitespaceFields_ThrowsException() {
        customer.setFirstName("   ");
        assertThrows(InvalidOperationException.class, () ->
                orderValidator.validateCustomerProfile(customer));
    }

    @Test
    void validateOrderCancellation_WithNullStatus_ThrowsException() {
        order.setStatus(null);
        assertThrows(NullPointerException.class, () ->
                orderValidator.validateOrderCancellation(order));
    }
}
