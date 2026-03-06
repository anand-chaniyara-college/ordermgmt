package com.example.ordermgmt.controller;

import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.Organization;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.OrganizationRepository;
import com.example.ordermgmt.repository.PricingCatalogRepository;
import com.example.ordermgmt.security.TenantContextHolder;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private PricingCatalogRepository pricingCatalogRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void cleanDatabase() {
        pricingCatalogRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        ensureRootTenantOrganizationExists();
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER")
    void getAvailableProducts_withoutPagination_returnsOnlyAvailablePricedItems() throws Exception {
        saveInventoryItem("Laptop", 10, new BigDecimal("999.99"));
        saveInventoryItem("OutOfStock", 0, new BigDecimal("199.99"));
        saveInventoryItem("NoPriceItem", 5, null);

        mockMvc.perform(get("/api/customer/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(1)))
                .andExpect(jsonPath("$.products[0].itemName").value("Laptop"))
                .andExpect(jsonPath("$.products[0].availableStock").value(10))
                .andExpect(jsonPath("$.products[0].unitPrice").value(999.99));
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER")
    void getAvailableProducts_withPagination_returnsPageResponse() throws Exception {
        saveInventoryItem("Item-A", 8, new BigDecimal("10.00"));
        saveInventoryItem("Item-B", 6, new BigDecimal("20.00"));
        saveInventoryItem("Item-C", 4, new BigDecimal("30.00"));
        saveInventoryItem("OutOfStock", 0, new BigDecimal("40.00"));

        mockMvc.perform(get("/api/customer/products")
                .param("page", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[*].availableStock", everyItem(greaterThan(0))))
                .andExpect(jsonPath("$.content[*].unitPrice", everyItem(notNullValue())));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void getAvailableProducts_withWrongAuthority_isForbidden() throws Exception {
        mockMvc.perform(get("/api/customer/products"))
                .andExpect(status().isForbidden());
    }

    private void saveInventoryItem(String itemName, int availableStock, BigDecimal unitPrice) {
        InventoryItem item = new InventoryItem();
        item.setItemName(itemName);
        item.setAvailableStock(availableStock);
        item.setReservedStock(0);

        if (unitPrice != null) {
            PricingCatalog pricingCatalog = new PricingCatalog();
            pricingCatalog.setInventoryItem(item);
            pricingCatalog.setUnitPrice(unitPrice);
            item.setPricingCatalog(pricingCatalog);
        }

        inventoryItemRepository.save(item);
    }

    private void ensureRootTenantOrganizationExists() {
        UUID rootOrgId = TenantContextHolder.ROOT_TENANT_ID;
        if (organizationRepository.existsById(rootOrgId)) {
            return;
        }

        Organization organization = new Organization();
        organization.setOrgId(rootOrgId);
        organization.setName("Root Test Org");
        organization.setSubdomain("root-test");
        organization.setDescription("Seeded for product catalog integration tests");
        organization.setIsActive(true);
        organizationRepository.save(organization);
    }
}
