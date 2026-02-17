package com.example.ordermgmt.config;

import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.OrderStatusLookup;
import com.example.ordermgmt.entity.UserRole;
import com.example.ordermgmt.repository.AppUserRepository;
import com.example.ordermgmt.repository.CustomerRepository;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.OrderStatusLookupRepository;
import com.example.ordermgmt.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    // Roles
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final int ROLE_ID_CUSTOMER = 1;
    private static final int ROLE_ID_ADMIN = 2;

    // Users
    private static final String EMAIL_ADMIN = "admin@example.com";
    private static final String PASS_ADMIN = "adminpassword";
    private static final String EMAIL_CUSTOMER = "customer@example.com";
    private static final String PASS_CUSTOMER = "password123";

    // Customer Profile
    private static final String CUST_FIRST_NAME = "John";
    private static final String CUST_LAST_NAME = "Doe";
    private static final String CUST_CONTACT = "1234567890";
    private static final String CUST_ADDRESS = "123 Main St";

    // Inventory
    private static final String ITEM_LAPTOP_ID = "ITEM001";
    private static final String ITEM_LAPTOP_NAME = "Laptop";
    private static final int ITEM_LAPTOP_STOCK = 100;

    // Order Statuses
    private static final List<String> ORDER_STATUSES = Arrays.asList(
            "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");

    @Bean
    public CommandLineRunner initData(OrderStatusLookupRepository statusRepository,
            AppUserRepository userRepository,
            CustomerRepository customerRepository,
            InventoryItemRepository inventoryRepository,
            UserRoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            logger.info("=================================================");
            logger.info("       STARTING DATA INITIALIZATION              ");
            logger.info("=================================================");

            initOrderStatuses(statusRepository);
            UserRole customerRole = initRole(roleRepository, ROLE_CUSTOMER, ROLE_ID_CUSTOMER);
            UserRole adminRole = initRole(roleRepository, ROLE_ADMIN, ROLE_ID_ADMIN);
            AppUser customerUser = initCustomerUser(userRepository, customerRole, passwordEncoder);
            initCustomerProfile(customerRepository, customerUser);
            initAdminUser(userRepository, adminRole, passwordEncoder);
            initInventory(inventoryRepository);

            logger.info("=================================================");
            logger.info("       DATA INITIALIZATION COMPLETED             ");
            logger.info("=================================================");
        };
    }

    private void initOrderStatuses(OrderStatusLookupRepository statusRepository) {
        logger.info("[INIT] Checking Order Statuses...");
        int id = 1;
        for (String statusName : ORDER_STATUSES) {
            if (statusRepository.findByStatusName(statusName).isEmpty()) {
                OrderStatusLookup status = new OrderStatusLookup();
                status.setStatusId(id++);
                status.setStatusName(statusName);
                statusRepository.save(status);
                logger.info("  [+] Created Order Status: {}", statusName);
            } else {
                logger.debug("  [-] Order Status exists: {}", statusName);
            }
        }
    }

    private UserRole initRole(UserRoleRepository roleRepository, String roleName, int roleId) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                    UserRole r = new UserRole();
                    r.setRoleId(roleId);
                    r.setRoleName(roleName);
                    UserRole saved = roleRepository.save(r);
                    logger.info("  [+] Created Role: {}", roleName);
                    return saved;
                });
    }

    private AppUser initCustomerUser(AppUserRepository userRepository, UserRole role, PasswordEncoder passwordEncoder) {
        logger.info("[INIT] Checking Default Customer User...");
        return userRepository.findByEmail(EMAIL_CUSTOMER)
                .map(existing -> {
                    logger.info("  [-] Customer User exists: {}", EMAIL_CUSTOMER);
                    return existing;
                })
                .orElseGet(() -> {
                    AppUser user = new AppUser();
                    user.setUserId(UUID.randomUUID().toString());
                    user.setEmail(EMAIL_CUSTOMER);
                    user.setPasswordHash(passwordEncoder.encode(PASS_CUSTOMER));
                    user.setRole(role);
                    user.setIsActive(true);
                    AppUser saved = userRepository.save(user);
                    logger.info("  [+] Created Customer User: {}", EMAIL_CUSTOMER);
                    return saved;
                });
    }

    private void initCustomerProfile(CustomerRepository customerRepository, AppUser user) {
        logger.info("[INIT] Checking Customer Profile...");
        if (customerRepository.findByAppUser(user).isEmpty()) {
            Customer customerProfile = new Customer();
            customerProfile.setCustomerId(UUID.randomUUID().toString());
            customerProfile.setAppUser(user);
            customerProfile.setFirstName(CUST_FIRST_NAME);
            customerProfile.setLastName(CUST_LAST_NAME);
            customerProfile.setContactNo(CUST_CONTACT);
            customerProfile.setAddress(CUST_ADDRESS);
            customerRepository.save(customerProfile);
            logger.info("  [+] Created Profile for: {}", user.getEmail());
        } else {
            logger.info("  [-] Profile exists for: {}", user.getEmail());
        }
    }

    private void initAdminUser(AppUserRepository userRepository, UserRole role, PasswordEncoder passwordEncoder) {
        logger.info("[INIT] Checking Default Admin User...");
        if (userRepository.findByEmail(EMAIL_ADMIN).isEmpty()) {
            AppUser adminUser = new AppUser();
            adminUser.setUserId(UUID.randomUUID().toString());
            adminUser.setEmail(EMAIL_ADMIN);
            adminUser.setPasswordHash(passwordEncoder.encode(PASS_ADMIN));
            adminUser.setRole(role);
            adminUser.setIsActive(true);
            userRepository.save(adminUser);
            logger.info("  [+] Created Admin User: {}", EMAIL_ADMIN);
        } else {
            logger.info("  [-] Admin User exists: {}", EMAIL_ADMIN);
        }
    }

    private void initInventory(InventoryItemRepository inventoryRepository) {
        logger.info("[INIT] Checking Inventory...");
        if (!inventoryRepository.existsById(ITEM_LAPTOP_ID)) {
            InventoryItem item1 = new InventoryItem();
            item1.setItemId(ITEM_LAPTOP_ID);
            item1.setItemName(ITEM_LAPTOP_NAME);
            item1.setAvailableStock(ITEM_LAPTOP_STOCK);
            item1.setReservedStock(0);
            inventoryRepository.save(item1);
            logger.info("  [+] Created Inventory Item: {} ({})", ITEM_LAPTOP_NAME, ITEM_LAPTOP_ID);
        } else {
            logger.info("  [-] Inventory Item exists: {}", ITEM_LAPTOP_ID);
        }
    }
}
