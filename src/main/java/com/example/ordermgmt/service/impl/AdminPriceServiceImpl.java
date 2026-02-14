package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.AdminPricingDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.entity.PricingHistory;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.PricingCatalogRepository;
import com.example.ordermgmt.repository.PricingHistoryRepository;
import com.example.ordermgmt.service.AdminPriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminPriceServiceImpl implements AdminPriceService {

    private static final Logger logger = LoggerFactory.getLogger(AdminPriceServiceImpl.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PricingCatalogRepository pricingCatalogRepository;
    private final PricingHistoryRepository pricingHistoryRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public AdminPriceServiceImpl(PricingCatalogRepository pricingCatalogRepository,
            PricingHistoryRepository pricingHistoryRepository,
            InventoryItemRepository inventoryItemRepository) {
        this.pricingCatalogRepository = pricingCatalogRepository;
        this.pricingHistoryRepository = pricingHistoryRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public List<AdminPricingDTO> getAllPrices() {
        return pricingCatalogRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminPricingDTO> getPriceHistory(String itemId) {
        return pricingHistoryRepository.findAllByInventoryItemItemIdOrderByCreatedTimestampDesc(itemId).stream()
                .map(this::convertHistoryToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String addPrice(AdminPricingDTO pricingDTO) {
        InventoryItem item = inventoryItemRepository.findById(pricingDTO.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found: " + pricingDTO.getItemId()));

        if (pricingCatalogRepository.existsById(pricingDTO.getItemId())) {
            return "Price already set for this item. Use update instead.";
        }

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // Save to Catalog
        PricingCatalog pricing = new PricingCatalog();
        pricing.setInventoryItem(item);
        pricing.setUnitPrice(pricingDTO.getUnitPrice());
        pricing.setUpdatedTimestamp(now);
        pricingCatalogRepository.save(pricing);

        // Save Initial History
        PricingHistory history = new PricingHistory();
        history.setInventoryItem(item);
        history.setOldPrice(null);
        history.setNewPrice(pricingDTO.getUnitPrice());
        history.setCreatedTimestamp(now);
        history.setChangedBy("ADMIN");
        pricingHistoryRepository.save(history);

        return "Price record added successfully at " + now.format(formatter);
    }

    @Override
    @Transactional
    public String updatePrice(AdminPricingDTO pricingDTO) {
        PricingCatalog target = pricingCatalogRepository.findByItemId(pricingDTO.getItemId())
                .orElseThrow(() -> new RuntimeException(
                        "No existing price record found for item: " + pricingDTO.getItemId()));

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // Capture old price
        java.math.BigDecimal oldPrice = target.getUnitPrice();

        // Update Catalog
        target.setUnitPrice(pricingDTO.getUnitPrice());
        target.setUpdatedTimestamp(now);
        pricingCatalogRepository.save(target);

        // Save History
        PricingHistory history = new PricingHistory();
        history.setInventoryItem(target.getInventoryItem());
        history.setOldPrice(oldPrice);
        history.setNewPrice(pricingDTO.getUnitPrice());
        history.setCreatedTimestamp(now);
        history.setChangedBy("ADMIN");
        pricingHistoryRepository.save(history);

        return "Price updated successfully for item: " + pricingDTO.getItemId();
    }

    private AdminPricingDTO convertToDTO(PricingCatalog pricing) {
        return new AdminPricingDTO(
                pricing.getInventoryItem().getItemId(),
                pricing.getInventoryItem().getItemName(),
                pricing.getUnitPrice(),
                pricing.getUpdatedTimestamp());
    }

    private AdminPricingDTO convertHistoryToDTO(PricingHistory history) {
        return new AdminPricingDTO(
                history.getInventoryItem().getItemId(),
                history.getInventoryItem().getItemName(),
                history.getNewPrice(),
                history.getCreatedTimestamp());
    }
}
