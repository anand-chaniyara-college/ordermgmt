package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.AdminPricingDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.PricingCatalogRepository;
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
    private final InventoryItemRepository inventoryItemRepository;

    public AdminPriceServiceImpl(PricingCatalogRepository pricingCatalogRepository,
            InventoryItemRepository inventoryItemRepository) {
        this.pricingCatalogRepository = pricingCatalogRepository;
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
        return pricingCatalogRepository.findAllByIdItemIdOrderByIdCreatedTimestampDesc(itemId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public String addPrice(AdminPricingDTO pricingDTO) {
        InventoryItem item = inventoryItemRepository.findById(pricingDTO.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found: " + pricingDTO.getItemId()));

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        PricingCatalog pricing = new PricingCatalog();
        pricing.setId(new PricingCatalog.PricingCatalogId(item.getItemId(), now));
        pricing.setInventoryItem(item);
        pricing.setUnitPrice(pricingDTO.getUnitPrice());

        pricingCatalogRepository.save(pricing);
        return "Price record added successfully at " + now.format(formatter);
    }

    @Override
    @Transactional
    public String updatePrice(AdminPricingDTO pricingDTO) {
        PricingCatalog target = pricingCatalogRepository
                .findFirstByIdItemIdOrderByIdCreatedTimestampDesc(pricingDTO.getItemId())
                .orElseThrow(() -> new RuntimeException(
                        "No existing price record found for item: " + pricingDTO.getItemId()));

        target.setUnitPrice(pricingDTO.getUnitPrice());
        pricingCatalogRepository.save(target);
        return "Price updated successfully for item: " + pricingDTO.getItemId();
    }

    private AdminPricingDTO convertToDTO(PricingCatalog pricing) {
        return new AdminPricingDTO(
                pricing.getId().getItemId(),
                pricing.getInventoryItem().getItemName(),
                pricing.getUnitPrice(),
                pricing.getId().getCreatedTimestamp());
    }
}
