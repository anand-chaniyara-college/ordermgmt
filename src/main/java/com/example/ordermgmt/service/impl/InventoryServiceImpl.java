package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceImpl.class);
    private final InventoryItemRepository inventoryItemRepository;

    public InventoryServiceImpl(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public List<InventoryItemDTO> getAllInventory() {
        logger.info("Fetching all inventory items");
        List<InventoryItem> entities = inventoryItemRepository.findAll();
        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public String addInventoryItem(InventoryItemDTO itemDTO) {
        logger.info("Adding new inventory item: {}", itemDTO.getItemId());

        if (inventoryItemRepository.existsById(itemDTO.getItemId())) {
            logger.warn("Item already exists: {}", itemDTO.getItemId());
            return "Item ID already exists";
        }

        InventoryItem item = convertToEntity(itemDTO);
        inventoryItemRepository.save(item);

        logger.info("Item added successfully: {}", itemDTO.getItemId());
        return "Item added successfully";
    }

    @Override
    public String updateInventoryItem(String itemId, InventoryItemDTO itemDTO) {
        logger.info("Updating inventory item: {}", itemId);

        return inventoryItemRepository.findById(itemId).map(existingItem -> {
            if (!itemId.equals(itemDTO.getItemId())) {
                throw new IllegalArgumentException("Item ID cannot be changed");
            }

            existingItem.setItemName(itemDTO.getItemName());
            existingItem.setAvailableStock(itemDTO.getAvailableStock());
            existingItem.setReservedStock(itemDTO.getReservedStock());

            inventoryItemRepository.save(existingItem);
            logger.info("Item updated successfully: {}", itemDTO.getItemId());
            return "Item updated successfully";

        }).orElseGet(() -> {
            logger.warn("Item not found for update: {}", itemId);
            return "Item not found";
        });
    }

    @Override
    public InventoryItemDTO getInventoryItem(String itemId) {
        logger.info("Fetching single inventory item: {}", itemId);

        return inventoryItemRepository.findById(itemId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Override
    public String deleteInventoryItem(String itemId) {
        logger.info("Deleting inventory item: {}", itemId);

        if (inventoryItemRepository.existsById(itemId)) {
            inventoryItemRepository.deleteById(itemId);
            logger.info("Item deleted successfully: {}", itemId);
            return "Item deleted successfully";
        } else {
            logger.warn("Item not found for deletion: {}", itemId);
            return "Item not found";
        }
    }

    private InventoryItemDTO convertToDTO(InventoryItem item) {
        return new InventoryItemDTO(item.getItemId(), item.getItemName(), item.getAvailableStock(),
                item.getReservedStock(),
                item.getPricingCatalog() != null ? item.getPricingCatalog().getUnitPrice() : null);
    }

    private InventoryItem convertToEntity(InventoryItemDTO dto) {
        InventoryItem item = new InventoryItem();
        item.setItemId(dto.getItemId());
        item.setItemName(dto.getItemName());
        item.setAvailableStock(dto.getAvailableStock());
        item.setReservedStock(dto.getReservedStock());
        return item;
    }
}
