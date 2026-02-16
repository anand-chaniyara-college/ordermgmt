package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.ProductDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);
    private static final int MIN_STOCK_THRESHOLD = 0;

    private final InventoryItemRepository inventoryItemRepository;

    public ProductServiceImpl(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAvailableProducts() {
        logger.info("Processing getAvailableProducts request");

        List<InventoryItem> items = inventoryItemRepository.findAll();

        if (items.isEmpty()) {
            logger.warn("No items found in inventory");
            return List.of();
        }

        List<ProductDTO> available = items.stream()
                .filter(this::isAvailable)
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        logger.info("getAvailableProducts completed successfully - Found {} products", available.size());
        return available;
    }

    private boolean isAvailable(InventoryItem item) {
        return item.getAvailableStock() != null && item.getAvailableStock() > MIN_STOCK_THRESHOLD &&
                item.getPricingCatalog() != null &&
                item.getPricingCatalog().getUnitPrice() != null &&
                item.getPricingCatalog().getUnitPrice().compareTo(BigDecimal.ZERO) > 0;
    }

    private ProductDTO convertToDTO(InventoryItem item) {
        PricingCatalog pricing = item.getPricingCatalog();
        BigDecimal unitPrice = (pricing != null) ? pricing.getUnitPrice() : null;

        return new ProductDTO(item.getItemId(), item.getItemName(), unitPrice, item.getAvailableStock());
    }
}
