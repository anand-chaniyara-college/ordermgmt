package com.example.ordermgmt.config;

import com.example.ordermgmt.entity.OrderStatusLookup;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.repository.OrderStatusLookupRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(OrderStatusLookupRepository statusRepository,
            com.example.ordermgmt.repository.AppUserRepository userRepository,
            com.example.ordermgmt.repository.CustomerRepository customerRepository,
            com.example.ordermgmt.repository.InventoryItemRepository inventoryRepository,
            com.example.ordermgmt.repository.UserRoleRepository roleRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Initialize Order Statuses
            List<String> statuses = Arrays.asList(
                    "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");

            int id = 1;
            for (String statusName : statuses) {
                if (statusRepository.findByStatusName(statusName).isEmpty()) {
                    OrderStatusLookup status = new OrderStatusLookup();
                    status.setStatusId(id++);
                    status.setStatusName(statusName);
                    statusRepository.save(status);
                }
            }

            // 2. Initialize Roles and Users

            // Roles
            UserRole customerRole = roleRepository.findByRoleName("CUSTOMER")
                    .orElseGet(() -> {
                        UserRole r = new UserRole();
                        r.setRoleId(1);
                        r.setRoleName("CUSTOMER");
                        return roleRepository.save(r);
                    });

            UserRole adminRole = roleRepository.findByRoleName("ADMIN")
                    .orElseGet(() -> {
                        UserRole r = new UserRole();
                        r.setRoleId(2);
                        r.setRoleName("ADMIN");
                        return roleRepository.save(r);
                    });

            // Customer
            com.example.ordermgmt.entity.AppUser customerUser = userRepository.findByEmail("customer@example.com")
                    .orElse(null);
            if (customerUser == null) {
                customerUser = new com.example.ordermgmt.entity.AppUser();
                customerUser.setUserId(java.util.UUID.randomUUID().toString());
                customerUser.setEmail("customer@example.com");
                customerUser.setPasswordHash(passwordEncoder.encode("password123"));
                customerUser.setRole(customerRole);
                customerUser.setIsActive(true);
                customerUser.setCreatedTimestamp(java.time.LocalDateTime.now());
                customerUser = userRepository.save(customerUser);
            }

            if (customerRepository.findByAppUser(customerUser).isEmpty()) {
                com.example.ordermgmt.entity.Customer customerProfile = new com.example.ordermgmt.entity.Customer();
                customerProfile.setCustomerId(java.util.UUID.randomUUID().toString());
                customerProfile.setAppUser(customerUser);
                customerProfile.setFirstName("John");
                customerProfile.setLastName("Doe");
                customerProfile.setContactNo("1234567890");
                customerProfile.setAddress("123 Main St");
                customerRepository.save(customerProfile);
            }

            // Admin
            if (userRepository.findByEmail("admin@example.com").isEmpty()) {
                com.example.ordermgmt.entity.AppUser adminUser = new com.example.ordermgmt.entity.AppUser();
                adminUser.setUserId(java.util.UUID.randomUUID().toString());
                adminUser.setEmail("admin@example.com");
                adminUser.setPasswordHash(passwordEncoder.encode("adminpassword"));
                adminUser.setRole(adminRole);
                adminUser.setIsActive(true);
                adminUser.setCreatedTimestamp(java.time.LocalDateTime.now());
                userRepository.save(adminUser);
            }

            // 3. Initialize Inventory
            if (!inventoryRepository.existsById("ITEM001")) {
                com.example.ordermgmt.entity.InventoryItem item1 = new com.example.ordermgmt.entity.InventoryItem();
                item1.setItemId("ITEM001");
                item1.setItemName("Laptop");
                item1.setAvailableStock(100);
                item1.setReservedStock(0);
                inventoryRepository.save(item1);
            }
        };
    }
}
