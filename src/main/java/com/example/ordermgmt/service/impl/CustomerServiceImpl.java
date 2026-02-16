package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.CustomerProfileDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.exception.ResourceNotFoundException;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.CustomerRepository;
import com.example.ordermgmt.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final AppUserRepository appUserRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository, AppUserRepository appUserRepository) {
        this.customerRepository = customerRepository;
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileDTO getCustomerProfile(String email) {
        logger.info("Processing getCustomerProfile for customer: {}", email);

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("getCustomerProfile failed for customer: {}: User not found", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });

        return customerRepository.findByAppUser(user)
                .map(this::convertToDTO)
                .orElseGet(() -> {
                    logger.warn(
                            "Processing getCustomerProfile for customer: {} - Customer record not found, returning default",
                            email);

                    CustomerProfileDTO dto = new CustomerProfileDTO();
                    dto.setEmail(email);
                    return dto;
                });
    }

    @Override
    @Transactional
    public String updateCustomerProfile(String email, CustomerProfileDTO profileDTO) {
        logger.info("Processing updateCustomerProfile for customer: {}", email);

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("updateCustomerProfile failed for customer: {}: User not found", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });

        Customer customer = customerRepository.findByAppUser(user)
                .orElseGet(() -> createNewCustomer(user));

        updateCustomerFields(customer, profileDTO);
        customerRepository.save(customer);

        logger.info("updateCustomerProfile completed successfully for customer: {}", email);
        return "Profile updated successfully";
    }

    private Customer createNewCustomer(AppUser user) {
        logger.info("Processing createNewCustomer for customer: {}", user.getEmail());
        Customer newCustomer = new Customer();
        newCustomer.setCustomerId(UUID.randomUUID().toString());
        newCustomer.setAppUser(user);
        return newCustomer;
    }

    private void updateCustomerFields(Customer customer, CustomerProfileDTO dto) {
        customer.setFirstName(dto.getFirstName());
        customer.setLastName(dto.getLastName());
        customer.setContactNo(dto.getContactNo());
        customer.setAddress(dto.getAddress());
    }

    private CustomerProfileDTO convertToDTO(Customer customer) {
        return new CustomerProfileDTO(
                customer.getFirstName(),
                customer.getLastName(),
                customer.getContactNo(),
                customer.getAddress(),
                customer.getAppUser().getEmail());
    }
}
