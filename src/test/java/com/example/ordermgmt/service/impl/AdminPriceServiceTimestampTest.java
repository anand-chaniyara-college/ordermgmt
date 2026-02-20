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
import java.time.LocalDateTime;
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
    }

    @Test
    void addPrices_NoEffectiveFrom_ShouldUseNow() {
        // Arrange
        pricingDTO = new AdminPricingDTO("item123", new BigDecimal("100.00"), null);

        when(pricingCatalogRepository.existsById("item123")).thenReturn(false);
        when(inventoryItemRepository.findById("item123")).thenReturn(Optional.of(item));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("test-user"));

        // Act
        adminPriceService.addPrices(Collections.singletonList(pricingDTO));

        // Assert
        ArgumentCaptor<PricingCatalog> catalogCaptor = ArgumentCaptor.forClass(PricingCatalog.class);
        verify(pricingCatalogRepository).save(catalogCaptor.capture());
        PricingCatalog savedCatalog = catalogCaptor.getValue();

        ArgumentCaptor<PricingHistory> historyCaptor = ArgumentCaptor.forClass(PricingHistory.class);
        verify(pricingHistoryRepository).save(historyCaptor.capture());
        PricingHistory savedHistory = historyCaptor.getValue();

        assertNotNull(savedCatalog.getUpdatedTimestamp());
        assertNotNull(savedHistory.getCreatedTimestamp());

        // They should be equal (syncTime)
        assertEquals(savedCatalog.getUpdatedTimestamp(), savedHistory.getCreatedTimestamp());

        // Since effectiveFrom was null, it should be close to NOW
        assertTrue(savedCatalog.getUpdatedTimestamp().isAfter(LocalDateTime.now().minusSeconds(5)));
        assertTrue(savedCatalog.getUpdatedTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void addPrices_WithEffectiveFrom_ShouldUseEffectiveFrom() {
        // Arrange
        LocalDateTime futureDate = LocalDateTime.now().plusDays(5);
        pricingDTO = new AdminPricingDTO("item123", new BigDecimal("100.00"), futureDate);

        when(pricingCatalogRepository.existsById("item123")).thenReturn(false);
        when(inventoryItemRepository.findById("item123")).thenReturn(Optional.of(item));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("test-user"));

        // Act
        adminPriceService.addPrices(Collections.singletonList(pricingDTO));

        // Assert
        ArgumentCaptor<PricingCatalog> catalogCaptor = ArgumentCaptor.forClass(PricingCatalog.class);
        verify(pricingCatalogRepository).save(catalogCaptor.capture());
        PricingCatalog savedCatalog = catalogCaptor.getValue();

        ArgumentCaptor<PricingHistory> historyCaptor = ArgumentCaptor.forClass(PricingHistory.class);
        verify(pricingHistoryRepository).save(historyCaptor.capture());
        PricingHistory savedHistory = historyCaptor.getValue();

        // Should match effectiveFrom
        assertEquals(futureDate, savedCatalog.getUpdatedTimestamp());
        assertEquals(futureDate, savedHistory.getCreatedTimestamp());
    }

    @Test
    void updatePrices_WithEffectiveFrom_ShouldUseEffectiveFrom() {
        // Arrange
        PricingCatalog existingCatalog = new PricingCatalog();
        existingCatalog.setInventoryItem(item);
        existingCatalog.setUnitPrice(new BigDecimal("50.00"));

        LocalDateTime pastDate = LocalDateTime.now().minusDays(2);
        pricingDTO = new AdminPricingDTO("item123", new BigDecimal("150.00"), pastDate);

        when(pricingCatalogRepository.findById("item123")).thenReturn(Optional.of(existingCatalog));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("test-user"));

        // Act
        adminPriceService.updatePrices(Collections.singletonList(pricingDTO));

        // Assert
        ArgumentCaptor<PricingCatalog> catalogCaptor = ArgumentCaptor.forClass(PricingCatalog.class);
        verify(pricingCatalogRepository).save(catalogCaptor.capture());
        PricingCatalog savedCatalog = catalogCaptor.getValue();

        ArgumentCaptor<PricingHistory> historyCaptor = ArgumentCaptor.forClass(PricingHistory.class);
        verify(pricingHistoryRepository).save(historyCaptor.capture());
        PricingHistory savedHistory = historyCaptor.getValue();

        // Should match effectiveFrom
        assertEquals(pastDate, savedCatalog.getUpdatedTimestamp());
        assertEquals(pastDate, savedHistory.getCreatedTimestamp());
    }
}
