package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.AdminPricingDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.entity.PricingHistory;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.PricingCatalogRepository;
import com.example.ordermgmt.repository.PricingHistoryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PricingLogicTest {

    @Mock
    private PricingCatalogRepository pricingCatalogRepository;

    @Mock
    private PricingHistoryRepository pricingHistoryRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private AuditorAware<String> auditorAware;

    @InjectMocks
    private AdminPriceServiceImpl adminPriceService;

    @Test
    public void testAddPrice() {
        // Setup
        String itemId = "ITEM-001";
        AdminPricingDTO dto = new AdminPricingDTO();
        dto.setItemId(itemId);
        dto.setUnitPrice(new BigDecimal("100.00"));

        InventoryItem item = new InventoryItem();
        item.setItemId(itemId);

        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(pricingCatalogRepository.existsById(itemId)).thenReturn(false);
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("user"));

        // Execute
        adminPriceService.addPrices(Collections.singletonList(dto));

        // Verify
        verify(pricingCatalogRepository, times(1)).save(any(PricingCatalog.class));
        verify(pricingHistoryRepository, times(1)).save(any(PricingHistory.class));
    }

    @Test
    public void testUpdatePrice() {
        // Setup
        String itemId = "ITEM-001";
        AdminPricingDTO dto = new AdminPricingDTO();
        dto.setItemId(itemId);
        dto.setUnitPrice(new BigDecimal("150.00"));

        PricingCatalog existing = new PricingCatalog();
        existing.setUnitPrice(new BigDecimal("100.00"));
        existing.setInventoryItem(new InventoryItem()); // Set inventory item to avoid NPE if needed

        when(pricingCatalogRepository.findById(itemId)).thenReturn(Optional.of(existing));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("user"));

        // Execute
        adminPriceService.updatePrices(Collections.singletonList(dto));

        // Verify
        verify(pricingCatalogRepository, times(1)).save(existing);
        verify(pricingHistoryRepository, times(1)).save(any(PricingHistory.class));
        Assertions.assertEquals(new BigDecimal("150.00"), existing.getUnitPrice());
    }

    @Test
    public void testGetPrice() {
        // Setup
        String itemId = "ITEM-001";
        InventoryItem item = new InventoryItem();
        item.setItemId(itemId);
        item.setItemName("Test Item");

        PricingCatalog pricing = new PricingCatalog();
        pricing.setUnitPrice(new BigDecimal("99.99"));
        pricing.setInventoryItem(item); // Link back if needed for mapping
        item.setPricingCatalog(pricing);

        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        // Execute
        AdminPricingDTO result = adminPriceService.getPrice(itemId);

        // Verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(itemId, result.getItemId());
        Assertions.assertEquals(new BigDecimal("99.99"), result.getUnitPrice());
    }
}
