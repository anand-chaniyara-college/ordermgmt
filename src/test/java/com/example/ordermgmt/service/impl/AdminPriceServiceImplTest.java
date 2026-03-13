package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.AdminPricingDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPriceServiceImplTest {

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

    private UUID itemId;
    private InventoryItem inventoryItem;
    private PricingCatalog pricingCatalog;
    private AdminPricingDTO pricingDTO;

    @BeforeEach
    void setUp() {
        itemId = UUID.randomUUID();
        
        inventoryItem = new InventoryItem();
        inventoryItem.setItemId(itemId);
        inventoryItem.setItemName("Test Item");
        
        pricingCatalog = new PricingCatalog();
        pricingCatalog.setInventoryItem(inventoryItem);
        pricingCatalog.setUnitPrice(BigDecimal.valueOf(100));
        pricingCatalog.setUpdatedTimestamp(LocalDateTime.now());
        
        inventoryItem.setPricingCatalog(pricingCatalog);
        
        pricingDTO = new AdminPricingDTO(itemId, BigDecimal.valueOf(150), LocalDateTime.now());
        
        lenient().when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("testuser"));
    }

    @Test
    void getAllPrices_AsList_ReturnsAllPrices() {
        when(inventoryItemRepository.findAll()).thenReturn(List.of(inventoryItem));

        List<AdminPricingDTO> result = adminPriceService.getAllPrices();

        assertEquals(1, result.size());
        assertEquals(itemId, result.get(0).getItemId());
        assertEquals(BigDecimal.valueOf(100), result.get(0).getUnitPrice());
    }

    @Test
    void getAllPrices_AsPage_ReturnsPagedPrices() {
        Page<InventoryItem> itemPage = new PageImpl<>(List.of(inventoryItem));
        when(inventoryItemRepository.findAll(any(Pageable.class))).thenReturn(itemPage);

        Page<AdminPricingDTO> result = adminPriceService.getAllPrices(PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals(itemId, result.getContent().get(0).getItemId());
    }

    @Test
    void getPrice_WithExistingItem_ReturnsPrice() {
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(inventoryItem));

        AdminPricingDTO result = adminPriceService.getPrice(itemId);

        assertNotNull(result);
        assertEquals(itemId, result.getItemId());
        assertEquals(BigDecimal.valueOf(100), result.getUnitPrice());
    }

    @Test
    void getPrice_WithNonExistingItem_ThrowsException() {
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminPriceService.getPrice(itemId));
    }

    @Test
    void addPrices_WithNewItems_AddsSuccessfully() {
        when(pricingCatalogRepository.existsById(itemId)).thenReturn(false);
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(inventoryItem));

        adminPriceService.addPrices(List.of(pricingDTO));

        verify(pricingCatalogRepository).save(any(PricingCatalog.class));
        verify(pricingHistoryRepository).save(any());
    }

    @Test
    void addPrices_WithExistingPrice_ThrowsException() {
        when(pricingCatalogRepository.existsById(itemId)).thenReturn(true);

        assertThrows(InvalidOperationException.class, () -> 
                adminPriceService.addPrices(List.of(pricingDTO)));
    }

    @Test
    void addPrices_WithNonExistingItem_ThrowsException() {
        when(pricingCatalogRepository.existsById(itemId)).thenReturn(false);
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                adminPriceService.addPrices(List.of(pricingDTO)));
    }

    @Test
    void addPrices_WithoutEffectiveFrom_UsesCurrentTime() {
        when(pricingCatalogRepository.existsById(itemId)).thenReturn(false);
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(inventoryItem));
        
        AdminPricingDTO dtoWithoutTime = new AdminPricingDTO(itemId, BigDecimal.valueOf(150), null);

        adminPriceService.addPrices(List.of(dtoWithoutTime));

        ArgumentCaptor<PricingCatalog> catalogCaptor = ArgumentCaptor.forClass(PricingCatalog.class);
        verify(pricingCatalogRepository).save(catalogCaptor.capture());
        
        assertNotNull(catalogCaptor.getValue().getUpdatedTimestamp());
    }

    @Test
    void updatePrices_WithExistingPrices_UpdatesSuccessfully() {
        when(pricingCatalogRepository.findById(itemId)).thenReturn(Optional.of(pricingCatalog));

        adminPriceService.updatePrices(List.of(pricingDTO));

        assertEquals(BigDecimal.valueOf(150), pricingCatalog.getUnitPrice());
        verify(pricingCatalogRepository).save(pricingCatalog);
        verify(pricingHistoryRepository).save(any());
    }

    @Test
    void updatePrices_WithNonExistingPrice_ThrowsException() {
        when(pricingCatalogRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                adminPriceService.updatePrices(List.of(pricingDTO)));
    }

    @Test
    void updatePrices_WithoutEffectiveFrom_UsesCurrentTime() {
        when(pricingCatalogRepository.findById(itemId)).thenReturn(Optional.of(pricingCatalog));
        
        AdminPricingDTO dtoWithoutTime = new AdminPricingDTO(itemId, BigDecimal.valueOf(150), null);

        adminPriceService.updatePrices(List.of(dtoWithoutTime));

        ArgumentCaptor<PricingCatalog> catalogCaptor = ArgumentCaptor.forClass(PricingCatalog.class);
        verify(pricingCatalogRepository).save(catalogCaptor.capture());
        
        assertNotNull(catalogCaptor.getValue().getUpdatedTimestamp());
    }

    @Test
    void convertItemToPricingDTO_WithoutPricingCatalog_ReturnsNullPrice() {
        InventoryItem itemWithoutPricing = new InventoryItem();
        itemWithoutPricing.setItemId(itemId);
        
        when(inventoryItemRepository.findAll()).thenReturn(List.of(itemWithoutPricing));

        List<AdminPricingDTO> result = adminPriceService.getAllPrices();

        assertEquals(1, result.size());
        assertNull(result.get(0).getUnitPrice());
        assertNull(result.get(0).getEffectiveFrom());
    }
}
