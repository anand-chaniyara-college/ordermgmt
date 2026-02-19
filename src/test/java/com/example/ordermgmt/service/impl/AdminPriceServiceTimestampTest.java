package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.AdminPricingDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.entity.PricingHistory;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.PricingCatalogRepository;
import com.example.ordermgmt.repository.PricingHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPriceServiceTimestampTest {

    @Mock
    private PricingCatalogRepository pricingCatalogRepository;

    @Mock
    private PricingHistoryRepository pricingHistoryRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private org.springframework.data.domain.AuditorAware<String> auditorAware;

    @InjectMocks
    private AdminPriceServiceImpl adminPriceService;

    private InventoryItem item;
    private AdminPricingDTO pricingDTO;

    @BeforeEach
    void setUp() {
        item = new InventoryItem();
        item.setItemId("item123");
        item.setItemName("Test Item");

        // Removed itemName from constructor
        pricingDTO = new AdminPricingDTO("item123", new BigDecimal("100.00"), null);
    }

    @Test
    void addPrice_shouldSynchronizeTimestampsAndSetAuditor() {
        // Arrange
        when(pricingCatalogRepository.existsById("item123")).thenReturn(false);
        when(inventoryItemRepository.findById("item123")).thenReturn(Optional.of(item));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("test-user@example.com"));

        // Act
        adminPriceService.addPrices(Collections.singletonList(pricingDTO));

        // Assert
        ArgumentCaptor<PricingCatalog> catalogCaptor = ArgumentCaptor.forClass(PricingCatalog.class);
        verify(pricingCatalogRepository).save(catalogCaptor.capture());
        PricingCatalog savedCatalog = catalogCaptor.getValue();

        ArgumentCaptor<PricingHistory> historyCaptor = ArgumentCaptor.forClass(PricingHistory.class);
        verify(pricingHistoryRepository).save(historyCaptor.capture());
        PricingHistory savedHistory = historyCaptor.getValue();

        assertNotNull(savedCatalog.getUpdatedTimestamp(), "Catalog updatedTimestamp should not be null");
        assertNotNull(savedHistory.getCreatedTimestamp(), "History createdTimestamp should not be null");
        assertEquals(savedCatalog.getUpdatedTimestamp(), savedHistory.getCreatedTimestamp(), "Timestamps must be exactly equal");
        assertEquals(savedCatalog.getCreatedTimestamp(), savedHistory.getCreatedTimestamp(), "Catalog createdTimestamp must also match history createdTimestamp");

        assertEquals("test-user@example.com", savedCatalog.getCreatedBy());
        assertEquals("test-user@example.com", savedCatalog.getUpdatedBy());
        assertEquals("test-user@example.com", savedHistory.getCreatedBy());
    }

    @Test
    void updatePrice_shouldSynchronizeTimestampsAndSetAuditor() {
        // Arrange
        PricingCatalog existingCatalog = new PricingCatalog();
        existingCatalog.setInventoryItem(item);
        existingCatalog.setUnitPrice(new BigDecimal("50.00"));

        when(pricingCatalogRepository.findById("item123")).thenReturn(Optional.of(existingCatalog));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("test-user@example.com"));

        // Removed itemName from constructor
        pricingDTO = new AdminPricingDTO("item123", new BigDecimal("150.00"), null);

        // Act
        adminPriceService.updatePrices(Collections.singletonList(pricingDTO));

        // Assert
        ArgumentCaptor<PricingCatalog> catalogCaptor = ArgumentCaptor.forClass(PricingCatalog.class);
        verify(pricingCatalogRepository).save(catalogCaptor.capture());
        PricingCatalog savedCatalog = catalogCaptor.getValue();

        ArgumentCaptor<PricingHistory> historyCaptor = ArgumentCaptor.forClass(PricingHistory.class);
        verify(pricingHistoryRepository).save(historyCaptor.capture());
        PricingHistory savedHistory = historyCaptor.getValue();

        assertNotNull(savedCatalog.getUpdatedTimestamp(), "Catalog updatedTimestamp should not be null");
        assertNotNull(savedHistory.getCreatedTimestamp(), "History createdTimestamp should not be null");
        assertEquals(savedCatalog.getUpdatedTimestamp(), savedHistory.getCreatedTimestamp(),
                "Timestamps must be exactly equal");

        assertEquals("test-user@example.com", savedCatalog.getUpdatedBy());
        assertEquals("test-user@example.com", savedHistory.getCreatedBy());
    }
}
