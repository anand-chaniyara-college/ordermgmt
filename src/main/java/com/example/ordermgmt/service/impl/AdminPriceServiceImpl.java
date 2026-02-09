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
        PricingCatalog target;

        // CASE 1: No effectiveFrom -> Find the LATEST record
        if (pricingDTO.getEffectiveFrom() == null) {
            logger.info("Admin: Updating LATEST price for item: {}", pricingDTO.getItemId());
            target = pricingCatalogRepository.findFirstByIdItemIdOrderByIdCreatedTimestampDesc(pricingDTO.getItemId())
                    .orElseThrow(() -> new RuntimeException(
                            "No existing price record found for item: " + pricingDTO.getItemId()));
        }
        // CASE 2: effectiveFrom provided -> Find that SPECIFIC record
        else {
            LocalDateTime oldTime = pricingDTO.getEffectiveFrom().truncatedTo(ChronoUnit.SECONDS);
            logger.info("Admin: Updating specific price for item: {} at {}", pricingDTO.getItemId(), oldTime);
            target = pricingCatalogRepository
                    .findById(new PricingCatalog.PricingCatalogId(pricingDTO.getItemId(), oldTime))
                    .orElseThrow(() -> new RuntimeException("No price record found for item " + pricingDTO.getItemId()
                            + " at " + oldTime.format(formatter)));
        }

        // Handle Time Update (Move record)
        if (pricingDTO.getNewEffectiveFrom() != null) {
            LocalDateTime newTime = pricingDTO.getNewEffectiveFrom().truncatedTo(ChronoUnit.SECONDS);

            // Re-create as Primary Keys are immutable
            PricingCatalog newRecord = new PricingCatalog();
            newRecord.setId(new PricingCatalog.PricingCatalogId(pricingDTO.getItemId(), newTime));
            newRecord.setInventoryItem(target.getInventoryItem());
            newRecord.setUnitPrice(pricingDTO.getUnitPrice());

            pricingCatalogRepository.delete(target);
            pricingCatalogRepository.save(newRecord);
            return "Price record moved and updated successfully to " + newTime.format(formatter);
        }

        // Standard update
        target.setUnitPrice(pricingDTO.getUnitPrice());
        pricingCatalogRepository.save(target);
        return "Price updated successfully for entry: " + target.getId().getCreatedTimestamp().format(formatter);
    }

    private AdminPricingDTO convertToDTO(PricingCatalog pricing) {
        return new AdminPricingDTO(
                pricing.getId().getItemId(),
                pricing.getUnitPrice(),
                pricing.getId().getCreatedTimestamp(),
                null);
    }
}
