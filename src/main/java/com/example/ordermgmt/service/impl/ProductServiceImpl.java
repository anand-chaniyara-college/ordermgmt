package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.ProductDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.entity.PricingCatalog;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.repository.PricingCatalogRepository;
import com.example.ordermgmt.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final InventoryItemRepository inventoryItemRepository;
    private final PricingCatalogRepository pricingCatalogRepository;

    public ProductServiceImpl(InventoryItemRepository inventoryItemRepository,
            PricingCatalogRepository pricingCatalogRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.pricingCatalogRepository = pricingCatalogRepository;
    }

    @Override
    public List<ProductDTO> getAvailableProducts() {
        logger.info("Fetching all available products for customer");

        List<InventoryItem> items = inventoryItemRepository.findAll();
        logger.info("Found {} items in inventory", items.size());

        List<ProductDTO> available = items.stream()
                .filter(item -> item.getAvailableStock() != null && item.getAvailableStock() > 0)
                .map(item -> {
                    BigDecimal price = pricingCatalogRepository
                            .findFirstByIdItemIdOrderByIdCreatedTimestampDesc(item.getItemId())
                            .map(PricingCatalog::getUnitPrice)
                            .orElse(BigDecimal.ZERO);

                    return new ProductDTO(item.getItemId(), item.getItemName(), price, item.getAvailableStock());
                })
                .collect(Collectors.toList());

        logger.info("Returning {} available products", available.size());
        return available;
    }
}
