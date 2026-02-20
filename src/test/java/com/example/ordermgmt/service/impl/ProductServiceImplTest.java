package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.ProductDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.repository.InventoryItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("should retriver available products with pricing")
    void getAvailableProducts_Success() {
        // Arrange
        String itemId = "ITEM-001";
        InventoryItem item = new InventoryItem();
        item.setItemId(itemId);
        item.setItemName("Test Product");
        item.setAvailableStock(10);

        PricingCatalog pricing = new PricingCatalog();
        pricing.setUnitPrice(new BigDecimal("99.99"));
        item.setPricingCatalog(pricing);

        when(inventoryItemRepository.findAvailableWithPricing()).thenReturn(List.of(item));

        // Act
        List<ProductDTO> result = productService.getAvailableProducts();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(itemId, result.get(0).getItemId());
        assertEquals(new BigDecimal("99.99"), result.get(0).getUnitPrice());

        verify(inventoryItemRepository, times(1)).findAvailableWithPricing();
    }

    @Test
    @DisplayName("should return empty list when no products available")
    void getAvailableProducts_Empty() {
        // Arrange
        when(inventoryItemRepository.findAvailableWithPricing()).thenReturn(Collections.emptyList());

        // Act
        List<ProductDTO> result = productService.getAvailableProducts();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(inventoryItemRepository, times(1)).findAvailableWithPricing();
    }
}
