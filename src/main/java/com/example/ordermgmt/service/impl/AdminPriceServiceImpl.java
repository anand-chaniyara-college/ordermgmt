package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.AdminPricingDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.entity.PricingHistory;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.ResourceNotFoundException;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.PricingCatalogRepository;
import com.example.ordermgmt.repository.PricingHistoryRepository;
import com.example.ordermgmt.service.AdminPriceService;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminPriceServiceImpl implements AdminPriceService {

    private static final Logger logger = LoggerFactory.getLogger(AdminPriceServiceImpl.class);

    private final PricingCatalogRepository pricingCatalogRepository;
    private final PricingHistoryRepository pricingHistoryRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final AuditorAware<String> auditorAware;

    public AdminPriceServiceImpl(PricingCatalogRepository pricingCatalogRepository,
            PricingHistoryRepository pricingHistoryRepository,
            InventoryItemRepository inventoryItemRepository,
            AuditorAware<String> auditorAware) {
        this.pricingCatalogRepository = pricingCatalogRepository;
        this.pricingHistoryRepository = pricingHistoryRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.auditorAware = auditorAware;
    }

    private String getCurrentAuditor() {
        return auditorAware.getCurrentAuditor().orElse("SYSTEM");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminPricingDTO> getAllPrices() {
        logger.info("Processing getAllPrices (List) for Admin");
        return inventoryItemRepository.findAll().stream()
                .map(this::convertItemToPricingDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPricingDTO> getAllPrices(Pageable pageable) {
        logger.info("Processing getAllPrices (Page) for Admin");
        return inventoryItemRepository.findAll(pageable)
                .map(this::convertItemToPricingDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPricingDTO getPrice(String itemId) {
        logger.info("Processing getPrice for Item: {}", itemId);
        return inventoryItemRepository.findById(itemId)
                .map(this::convertItemToPricingDTO)
                .orElseThrow(() -> {
                    logger.error("getPrice failed for Item: {} - Item not found", itemId);
                    return new ResourceNotFoundException("Item not found: " + itemId);
                });
    }

    @Override
    @Transactional
    public void addPrices(List<AdminPricingDTO> prices) {
        logger.info("Processing addPrices for {} items", prices.size());

        for (AdminPricingDTO pricingDTO : prices) {
            if (pricingCatalogRepository.existsById(pricingDTO.getItemId())) {
                logger.warn("Skipping addPrice for Item: {} - Price already set", pricingDTO.getItemId());
                throw new InvalidOperationException("Price already set for item: " + pricingDTO.getItemId());
            }

            InventoryItem item = inventoryItemRepository.findById(pricingDTO.getItemId())
                    .orElseThrow(() -> {
                        logger.error("addPrice failed for Item: {} - Item not found", pricingDTO.getItemId());
                        return new ResourceNotFoundException("Item not found: " + pricingDTO.getItemId());
                    });

            LocalDateTime now = LocalDateTime.now();
            String currentUser = getCurrentAuditor();

            PricingCatalog pricing = new PricingCatalog();
            pricing.setInventoryItem(item);
            pricing.setUnitPrice(pricingDTO.getUnitPrice());
            pricing.setUpdatedTimestamp(now);
            pricing.setCreatedBy(currentUser);
            pricing.setUpdatedBy(currentUser);
            pricing.setCreatedTimestamp(now);

            pricingCatalogRepository.save(pricing);
            savePricingHistory(item, null, pricingDTO.getUnitPrice(), now, currentUser);
        }
        logger.info("addPrices completed successfully");
    }

    @Override
    @Transactional
    public void updatePrices(List<AdminPricingDTO> prices) {
        logger.info("Processing updatePrices for {} items", prices.size());

        for (AdminPricingDTO pricingDTO : prices) {
            PricingCatalog target = pricingCatalogRepository.findById(pricingDTO.getItemId())
                    .orElseThrow(() -> {
                        logger.error("updatePrice failed for Item: {} - Price record not found",
                                pricingDTO.getItemId());
                        return new ResourceNotFoundException(
                                "No existing price record found for item: " + pricingDTO.getItemId());
                    });

            BigDecimal oldPrice = target.getUnitPrice();
            LocalDateTime now = LocalDateTime.now();
            String currentUser = getCurrentAuditor();

            target.setUnitPrice(pricingDTO.getUnitPrice());
            target.setUpdatedTimestamp(now);
            target.setUpdatedBy(currentUser);

            pricingCatalogRepository.save(target);
            savePricingHistory(target.getInventoryItem(), oldPrice, pricingDTO.getUnitPrice(), now, currentUser);
        }
        logger.info("updatePrices completed successfully");
    }

    private void savePricingHistory(InventoryItem item, BigDecimal oldPrice, BigDecimal newPrice,
            LocalDateTime timestamp, String auditor) {
        PricingHistory history = new PricingHistory();
        history.setInventoryItem(item);
        history.setOldPrice(oldPrice);
        history.setNewPrice(newPrice);
        history.setCreatedTimestamp(timestamp);
        history.setCreatedBy(auditor);
        pricingHistoryRepository.save(history);
    }

    private AdminPricingDTO convertItemToPricingDTO(InventoryItem item) {
        BigDecimal unitPrice = null;
        LocalDateTime updatedTimestamp = null;

        if (item.getPricingCatalog() != null) {
            unitPrice = item.getPricingCatalog().getUnitPrice();
            updatedTimestamp = item.getPricingCatalog().getUpdatedTimestamp();
        }

        return new AdminPricingDTO(
                item.getItemId(),
                unitPrice,
                updatedTimestamp);
    }
}
