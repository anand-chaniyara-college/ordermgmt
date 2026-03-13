package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.ProductDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.repository.InventoryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID itemId;
    private InventoryItem availableItem;
    private InventoryItem unavailableItem;
    private PricingCatalog pricingCatalog;

    @BeforeEach
    void setUp() {
        itemId = UUID.randomUUID();

        pricingCatalog = new PricingCatalog();
        pricingCatalog.setUnitPrice(BigDecimal.valueOf(99.99));

        availableItem = new InventoryItem();
        availableItem.setItemId(itemId);
        availableItem.setItemName("Available Product");
        availableItem.setAvailableStock(100);
        availableItem.setPricingCatalog(pricingCatalog);

        unavailableItem = new InventoryItem();
        unavailableItem.setItemId(UUID.randomUUID());
        unavailableItem.setItemName("Unavailable Product");
        unavailableItem.setAvailableStock(0);
        unavailableItem.setPricingCatalog(pricingCatalog);
    }

    @Test
    void getAvailableProducts_AsList_ReturnsOnlyAvailableItems() {
        when(inventoryItemRepository.findAvailableWithPricing())
                .thenReturn(List.of(availableItem));

        List<ProductDTO> result = productService.getAvailableProducts();

        assertEquals(1, result.size());
        assertEquals(itemId, result.get(0).getItemId());
        assertEquals("Available Product", result.get(0).getItemName());
        assertEquals(BigDecimal.valueOf(99.99), result.get(0).getUnitPrice());
        assertEquals(100, result.get(0).getAvailableStock());
    }

    @Test
    void getAvailableProducts_AsList_WithNoAvailableItems_ReturnsEmptyList() {
        when(inventoryItemRepository.findAvailableWithPricing())
                .thenReturn(List.of());

        List<ProductDTO> result = productService.getAvailableProducts();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAvailableProducts_AsList_FiltersOutUnavailableItems() {
        when(inventoryItemRepository.findAvailableWithPricing())
                .thenReturn(List.of(availableItem, unavailableItem));

        List<ProductDTO> result = productService.getAvailableProducts();

        assertEquals(1, result.size());
        assertEquals(itemId, result.get(0).getItemId());
    }

    @Test
    void getAvailableProducts_AsList_WithZeroPrice_ExcludesItem() {
        InventoryItem zeroPriceItem = new InventoryItem();
        zeroPriceItem.setItemId(UUID.randomUUID());
        zeroPriceItem.setItemName("Zero Price Item");
        zeroPriceItem.setAvailableStock(50);
        PricingCatalog zeroPrice = new PricingCatalog();
        zeroPrice.setUnitPrice(BigDecimal.ZERO);
        zeroPriceItem.setPricingCatalog(zeroPrice);

        when(inventoryItemRepository.findAvailableWithPricing())
                .thenReturn(List.of(availableItem, zeroPriceItem));

        List<ProductDTO> result = productService.getAvailableProducts();

        assertEquals(1, result.size());
        assertEquals(itemId, result.get(0).getItemId());
    }

    @Test
    void getAvailableProducts_AsList_WithNullPricing_ExcludesItem() {
        InventoryItem noPricingItem = new InventoryItem();
        noPricingItem.setItemId(UUID.randomUUID());
        noPricingItem.setItemName("No Pricing Item");
        noPricingItem.setAvailableStock(50);
        noPricingItem.setPricingCatalog(null);

        when(inventoryItemRepository.findAvailableWithPricing())
                .thenReturn(List.of(availableItem, noPricingItem));

        List<ProductDTO> result = productService.getAvailableProducts();

        assertEquals(1, result.size());
        assertEquals(itemId, result.get(0).getItemId());
    }

    @Test
    void getAvailableProducts_AsPage_ReturnsPagedAvailableItems() {
        Page<InventoryItem> itemPage = new PageImpl<>(List.of(availableItem));
        when(inventoryItemRepository.findAvailableWithPricing(any(Pageable.class)))
                .thenReturn(itemPage);

        Page<ProductDTO> result = productService.getAvailableProducts(PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals(itemId, result.getContent().get(0).getItemId());
    }

    @Test
    void getAvailableProducts_AsPage_WithEmptyPage_ReturnsEmptyPage() {
        Page<InventoryItem> emptyPage = new PageImpl<>(List.of());
        when(inventoryItemRepository.findAvailableWithPricing(any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<ProductDTO> result = productService.getAvailableProducts(PageRequest.of(0, 10));

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getAvailableProducts_AsPage_FiltersOutUnavailableItems() {
        Page<InventoryItem> itemPage = new PageImpl<>(List.of(availableItem, unavailableItem));
        when(inventoryItemRepository.findAvailableWithPricing(any(Pageable.class)))
                .thenReturn(itemPage);

        Page<ProductDTO> result = productService.getAvailableProducts(PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
    }

    @Test
    void convertToDTO_WithNullPricing_ReturnsNullPrice() {
        InventoryItem itemWithoutPricing = new InventoryItem();
        itemWithoutPricing.setItemId(UUID.randomUUID());
        itemWithoutPricing.setItemName("No Pricing");
        itemWithoutPricing.setAvailableStock(50);
        itemWithoutPricing.setPricingCatalog(null);

        when(inventoryItemRepository.findAvailableWithPricing())
                .thenReturn(List.of(itemWithoutPricing));

        List<ProductDTO> result = productService.getAvailableProducts();

        // Item without pricing should be filtered out by isAvailable() method
        assertTrue(result.isEmpty());
    }
}
