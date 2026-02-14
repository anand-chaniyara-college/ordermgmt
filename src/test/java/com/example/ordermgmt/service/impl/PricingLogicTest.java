package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.AdminPricingDTO;
import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.entity.PricingHistory;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.PricingCatalogRepository;
import com.example.ordermgmt.repository.PricingHistoryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    @InjectMocks
    private AdminPriceServiceImpl adminPriceService;

    @Test
    public void testAddPrice() {
        // Setup
        String itemId = "ITEM-001";
        BigDecimal price = new BigDecimal("100.00");
        InventoryItem item = new InventoryItem();
        item.setItemId(itemId);
        item.setItemName("Test Item");

        AdminPricingDTO dto = new AdminPricingDTO();
        dto.setItemId(itemId);
        dto.setUnitPrice(price);

        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(pricingCatalogRepository.existsById(itemId)).thenReturn(false);

        // Execute
        String result = adminPriceService.addPrice(dto);

        // Verify
        Assertions.assertTrue(result.contains("added successfully"));

        // Check Catalog Save
        ArgumentCaptor<PricingCatalog> catalogCaptor = ArgumentCaptor.forClass(PricingCatalog.class);
        verify(pricingCatalogRepository).save(catalogCaptor.capture());
        Assertions.assertEquals(price, catalogCaptor.getValue().getUnitPrice());
        Assertions.assertEquals(itemId, catalogCaptor.getValue().getInventoryItem().getItemId());

        // Check History Save
        ArgumentCaptor<PricingHistory> historyCaptor = ArgumentCaptor.forClass(PricingHistory.class);
        verify(pricingHistoryRepository).save(historyCaptor.capture());
        Assertions.assertEquals(price, historyCaptor.getValue().getNewPrice());
        Assertions.assertNull(historyCaptor.getValue().getOldPrice());
    }

    @Test
    public void testUpdatePrice() {
        // Setup
        String itemId = "ITEM-002";
        BigDecimal oldPrice = new BigDecimal("50.00");
        BigDecimal newPrice = new BigDecimal("75.00");

        InventoryItem item = new InventoryItem();
        item.setItemId(itemId);

        PricingCatalog currentCatalog = new PricingCatalog();
        currentCatalog.setItemId(itemId);
        currentCatalog.setInventoryItem(item);
        currentCatalog.setUnitPrice(oldPrice);

        AdminPricingDTO dto = new AdminPricingDTO();
        dto.setItemId(itemId);
        dto.setUnitPrice(newPrice);

        when(pricingCatalogRepository.findByItemId(itemId)).thenReturn(Optional.of(currentCatalog));

        // Execute
        String result = adminPriceService.updatePrice(dto);

        // Verify
        Assertions.assertTrue(result.contains("updated successfully"));

        // Check Catalog Update
        ArgumentCaptor<PricingCatalog> catalogCaptor = ArgumentCaptor.forClass(PricingCatalog.class);
        verify(pricingCatalogRepository).save(catalogCaptor.capture());
        Assertions.assertEquals(newPrice, catalogCaptor.getValue().getUnitPrice());

        // Check History Save
        ArgumentCaptor<PricingHistory> historyCaptor = ArgumentCaptor.forClass(PricingHistory.class);
        verify(pricingHistoryRepository).save(historyCaptor.capture());
        Assertions.assertEquals(oldPrice, historyCaptor.getValue().getOldPrice());
        Assertions.assertEquals(newPrice, historyCaptor.getValue().getNewPrice());
    }
}
