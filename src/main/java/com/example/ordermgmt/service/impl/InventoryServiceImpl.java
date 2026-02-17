package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.ResourceNotFoundException;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceImpl.class);
    private final InventoryItemRepository inventoryItemRepository;

    public InventoryServiceImpl(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemDTO> getAllInventory() {
        logger.info("Processing getAllInventory for Admin");
        List<InventoryItem> entities = inventoryItemRepository.findAll();
        List<InventoryItemDTO> result = entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        logger.info("getAllInventory completed successfully for Admin - Retrieved {} items", result.size());
        return result;
    }

    @Override
    public String addInventoryItem(InventoryItemDTO itemDTO) {
        logger.info("Processing addInventoryItem for Item: {}", itemDTO.getItemId());

        if (inventoryItemRepository.existsById(itemDTO.getItemId())) {
            logger.warn("Skipping addInventoryItem for Item: {} - Item ID already exists", itemDTO.getItemId());
            throw new InvalidOperationException("Item with ID " + itemDTO.getItemId() + " already exists");
        }

        InventoryItem item = convertToEntity(itemDTO);
        inventoryItemRepository.save(item);

        logger.info("addInventoryItem completed successfully for Item: {}", itemDTO.getItemId());
        return "Item added successfully";
    }

    @Override
    public String updateInventoryItem(String itemId, InventoryItemDTO itemDTO) {
        logger.info("Processing updateInventoryItem for Item: {}", itemId);

        if (!itemId.equals(itemDTO.getItemId())) {
            logger.warn("Skipping updateInventoryItem for Item: {} - ID mismatch in path and body", itemId);
            throw new InvalidOperationException("Item ID in path must match Item ID in body");
        }

        InventoryItem existingItem = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> {
                    logger.warn("Skipping updateInventoryItem for Item: {} - Item not found", itemId);
                    return new ResourceNotFoundException("Inventory Item not found with ID: " + itemId);
                });

        existingItem.setItemName(itemDTO.getItemName());
        existingItem.setAvailableStock(itemDTO.getAvailableStock());
        existingItem.setReservedStock(itemDTO.getReservedStock());

        inventoryItemRepository.save(existingItem);
        logger.info("updateInventoryItem completed successfully for Item: {}", itemId);
        return "Item updated successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemDTO getInventoryItem(String itemId) {
        logger.info("Processing getInventoryItem for Item: {}", itemId);

        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> {
                    logger.warn("Skipping getInventoryItem for Item: {} - Item not found", itemId);
                    return new ResourceNotFoundException("Inventory Item not found with ID: " + itemId);
                });

        InventoryItemDTO dto = convertToDTO(item);
        logger.info("getInventoryItem completed successfully for Item: {}", itemId);
        return dto;
    }

    @Override
    public String deleteInventoryItem(String itemId) {
        logger.info("Processing deleteInventoryItem for Item: {}", itemId);

        if (!inventoryItemRepository.existsById(itemId)) {
            logger.warn("Skipping deleteInventoryItem for Item: {} - Item not found", itemId);
            throw new ResourceNotFoundException("Inventory Item not found with ID: " + itemId);
        }

        inventoryItemRepository.deleteById(itemId);
        logger.info("deleteInventoryItem completed successfully for Item: {}", itemId);
        return "Item deleted successfully";
    }

    private InventoryItemDTO convertToDTO(InventoryItem item) {
        return new InventoryItemDTO(
                item.getItemId(),
                item.getItemName(),
                item.getAvailableStock(),
                item.getReservedStock());
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
