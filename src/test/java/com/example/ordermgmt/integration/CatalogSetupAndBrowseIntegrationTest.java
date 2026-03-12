package com.example.ordermgmt.integration;

import com.example.ordermgmt.dto.AddStockRequestDTO;
import com.example.ordermgmt.dto.AddStockWrapperDTO;
import com.example.ordermgmt.dto.AdminPricingDTO;
import com.example.ordermgmt.dto.AdminPricingWrapperDTO;
import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.dto.InventoryItemWrapperDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.Organization;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.entity.PricingHistory;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.OrganizationRepository;
import com.example.ordermgmt.repository.PricingCatalogRepository;
import com.example.ordermgmt.repository.PricingHistoryRepository;
import com.example.ordermgmt.security.TenantContextHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.BeanOverrideTestExecutionListener;
import org.springframework.test.context.event.ApplicationEventsTestExecutionListener;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.CommonCachesTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestExecutionListeners({
        ServletTestExecutionListener.class,
        DirtiesContextBeforeModesTestExecutionListener.class,
        ApplicationEventsTestExecutionListener.class,
        BeanOverrideTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        WithSecurityContextTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        CommonCachesTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        SqlScriptsTestExecutionListener.class,
        EventPublishingTestExecutionListener.class
})
class CatalogSetupAndBrowseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private PricingCatalogRepository pricingCatalogRepository;

    @Autowired
    private PricingHistoryRepository pricingHistoryRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
        pricingHistoryRepository.deleteAll();
        pricingCatalogRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        organizationRepository.deleteAll();
        ensureRootTenantOrganizationExists();
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = "ADMIN")
    void sq25_addInventoryItems_createsRowsAndReturnsIds() throws Exception {
        InventoryItemWrapperDTO request = new InventoryItemWrapperDTO(List.of(
                new InventoryItemDTO(null, "Laptop", 10, 0),
                new InventoryItemDTO(null, "Mouse", 25, 0)));

        MvcResult result = mockMvc.perform(post("/api/admin/inventory")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(body.get("items")).hasSize(2);
        assertThat(inventoryItemRepository.count()).isEqualTo(2);
        assertThat(inventoryItemRepository.findAll())
                .extracting(InventoryItem::getItemName)
                .containsExactlyInAnyOrder("Laptop", "Mouse");
        assertThat(inventoryItemRepository.findAll())
                .extracting(InventoryItem::getOrgId)
                .containsOnly(TenantContextHolder.ROOT_TENANT_ID);
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = "ADMIN")
    void sq26_sq27_updateInventoryAndAddStock_persistsChanges() throws Exception {
        InventoryItem item = saveInventoryItem("Laptop", 10, null);

        InventoryItemWrapperDTO updateRequest = new InventoryItemWrapperDTO(List.of(
                new InventoryItemDTO(item.getItemId(), "Laptop Pro", 12, 0)));

        mockMvc.perform(put("/api/admin/inventory")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0]").value(item.getItemId().toString()));

        AddStockWrapperDTO addStockRequest = new AddStockWrapperDTO(List.of(
                new AddStockRequestDTO(item.getItemId(), 5)));

        mockMvc.perform(post("/api/admin/inventory/addstock")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addStockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0]").value(item.getItemId().toString()));

        InventoryItem updatedItem = inventoryItemRepository.findById(item.getItemId()).orElseThrow();
        assertThat(updatedItem.getItemName()).isEqualTo("Laptop Pro");
        assertThat(updatedItem.getAvailableStock()).isEqualTo(17);
        assertThat(updatedItem.getReservedStock()).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = "ADMIN")
    void sq28_sq29_addAndUpdatePrices_writeCatalogAndHistory() throws Exception {
        InventoryItem item = saveInventoryItem("Laptop", 10, null);

        AdminPricingWrapperDTO createRequest = new AdminPricingWrapperDTO(List.of(
                new AdminPricingDTO(item.getItemId(), new BigDecimal("999.99"), LocalDateTime.of(2026, 3, 1, 10, 0))));

        mockMvc.perform(post("/api/admin/prices")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Price records added successfully."));

        PricingCatalog createdPrice = pricingCatalogRepository.findById(item.getItemId()).orElseThrow();
        List<PricingHistory> createHistory = pricingHistoryRepository.findAll();

        assertThat(createdPrice.getUnitPrice()).isEqualByComparingTo("999.99");
        assertThat(createHistory).hasSize(1);
        assertThat(createHistory.get(0).getOldPrice()).isNull();
        assertThat(createHistory.get(0).getNewPrice()).isEqualByComparingTo("999.99");

        AdminPricingWrapperDTO updateRequest = new AdminPricingWrapperDTO(List.of(
                new AdminPricingDTO(item.getItemId(), new BigDecimal("1099.99"), LocalDateTime.of(2026, 3, 2, 11, 30))));

        mockMvc.perform(put("/api/admin/prices")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Prices updated successfully."));

        PricingCatalog updatedPrice = pricingCatalogRepository.findById(item.getItemId()).orElseThrow();
        List<PricingHistory> historyRows = pricingHistoryRepository.findAll();

        assertThat(updatedPrice.getUnitPrice()).isEqualByComparingTo("1099.99");
        assertThat(historyRows).hasSize(2);
        assertThat(historyRows)
                .anySatisfy(history -> {
                    assertThat(history.getOldPrice()).isNull();
                    assertThat(history.getNewPrice()).isEqualByComparingTo("999.99");
                })
                .anySatisfy(history -> {
                    assertThat(history.getOldPrice()).isEqualByComparingTo("999.99");
                    assertThat(history.getNewPrice()).isEqualByComparingTo("1099.99");
                });
    }

    @Test
    @WithMockUser(username = "customer@example.com", authorities = "CUSTOMER")
    void sq30_sq31_customerBrowse_returnsOnlyPricedAvailableItems_withoutAndWithPagination() throws Exception {
        saveInventoryItem("Item-A", 8, new BigDecimal("10.00"));
        saveInventoryItem("Item-B", 6, new BigDecimal("20.00"));
        saveInventoryItem("Item-C", 4, new BigDecimal("30.00"));
        saveInventoryItem("OutOfStock", 0, new BigDecimal("40.00"));
        saveInventoryItem("NoPriceItem", 5, null);

        mockMvc.perform(get("/api/customer/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(3)))
                .andExpect(jsonPath("$.products[*].itemName", containsInAnyOrder("Item-A", "Item-B", "Item-C")))
                .andExpect(jsonPath("$.products[*].availableStock", everyItem(greaterThan(0))))
                .andExpect(jsonPath("$.products[*].unitPrice", everyItem(notNullValue())));

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
    @WithMockUser(username = "admin@example.com", authorities = "ADMIN")
    void sq30_negative_customerCatalogWithAdminAuthority_isForbidden() throws Exception {
        mockMvc.perform(get("/api/customer/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = "ADMIN")
    void sq32_duplicateInitialPrice_returnsBadRequestAndDoesNotCreateExtraHistory() throws Exception {
        InventoryItem item = saveInventoryItem("Keyboard", 9, null);
        AdminPricingWrapperDTO request = new AdminPricingWrapperDTO(List.of(
                new AdminPricingDTO(item.getItemId(), new BigDecimal("49.99"), null)));

        mockMvc.perform(post("/api/admin/prices")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/prices")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Price already set for item: " + item.getItemId()));

        assertThat(pricingCatalogRepository.count()).isEqualTo(1);
        assertThat(pricingHistoryRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = "ADMIN")
    void sq32_updateMissingPriceRecord_returnsNotFound() throws Exception {
        UUID missingItemId = UUID.randomUUID();
        AdminPricingWrapperDTO request = new AdminPricingWrapperDTO(List.of(
                new AdminPricingDTO(missingItemId, new BigDecimal("19.99"), null)));

        mockMvc.perform(put("/api/admin/prices")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No existing price record found for item: " + missingItemId));

        assertThat(pricingCatalogRepository.count()).isZero();
        assertThat(pricingHistoryRepository.count()).isZero();
    }

    private InventoryItem saveInventoryItem(String itemName, int availableStock, BigDecimal unitPrice) {
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

        return inventoryItemRepository.save(item);
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
        organization.setDescription("Seeded for catalog integration tests");
        organization.setIsActive(true);
        organizationRepository.save(organization);
    }
}
