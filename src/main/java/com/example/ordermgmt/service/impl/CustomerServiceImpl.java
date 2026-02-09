package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.CustomerProfileDTO;
import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.CustomerRepository;
import com.example.ordermgmt.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    public CustomerProfileDTO getCustomerProfile(String email) {
        logger.info("Fetching customer profile for: {}", email);

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return customerRepository.findByAppUser(user)
                .map(this::convertToDTO)
                .orElseGet(() -> {
                    logger.warn("Customer record not found for user: {}", email);
                    // Return empty profile with just email if not yet created
                    CustomerProfileDTO dto = new CustomerProfileDTO();
                    dto.setEmail(email);
                    return dto;
                });
    }

    @Override
    public String updateCustomerProfile(String email, CustomerProfileDTO profileDTO) {
        logger.info("Updating customer profile for: {}", email);

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Customer customer = customerRepository.findByAppUser(user)
                .orElseGet(() -> {
                    logger.info("Creating new customer record for user: {}", email);
                    Customer newCustomer = new Customer();
                    newCustomer.setCustomerId(UUID.randomUUID().toString());
                    newCustomer.setAppUser(user);
                    return newCustomer;
                });

        customer.setFirstName(profileDTO.getFirstName());
        customer.setLastName(profileDTO.getLastName());
        customer.setContactNo(profileDTO.getContactNo());

        customerRepository.save(customer);

        logger.info("Customer profile updated successfully for: {}", email);
        return "Profile updated successfully";
    }

    private CustomerProfileDTO convertToDTO(Customer customer) {
        return new CustomerProfileDTO(
                customer.getFirstName(),
                customer.getLastName(),
                customer.getContactNo(),
                customer.getAppUser().getEmail());
    }
}
