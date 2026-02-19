package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.exception.ResourceNotFoundException;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.ordermgmt.dto.AddStockRequestDTO;

import java.util.ArrayList;
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
        logger.info("Processing getAllInventory (List) for Admin");
        List<InventoryItemDTO> result = inventoryItemRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        logger.info("getAllInventory (List) completed successfully for Admin - Retrieved {} items", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryItemDTO> getAllInventory(Pageable pageable) {
        logger.info("Processing getAllInventory (Page) for Admin");
        Page<InventoryItemDTO> result = inventoryItemRepository.findAll(pageable)
                .map(this::convertToDTO);
        logger.info("getAllInventory (Page) completed successfully for Admin - Retrieved {} items",
                result.getNumberOfElements());
        return result;
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
    public List<String> addInventoryItems(List<InventoryItemDTO> items) {
        logger.info("Processing addInventoryItems for {} items", items.size());
        List<InventoryItem> entitiesToSave = new ArrayList<>();
        List<String> savedIds = new ArrayList<>();

        for (InventoryItemDTO dto : items) {
            // Validate itemName is present for creation
            if (dto.getItemName() == null || dto.getItemName().trim().isEmpty()) {
                throw new InvalidOperationException("Item Name is required for creating new item: " + dto.getItemId());
            }

            if (inventoryItemRepository.existsById(dto.getItemId())) {
                logger.warn("Skipping addInventoryItems - Item ID {} already exists", dto.getItemId());
                throw new InvalidOperationException("Item with ID " + dto.getItemId() + " already exists");
            }
            entitiesToSave.add(convertToEntity(dto));
            savedIds.add(dto.getItemId());
        }

        inventoryItemRepository.saveAll(entitiesToSave);
        logger.info("addInventoryItems completed successfully for {} items", items.size());
        return savedIds;
    }

    @Override
    public List<String> updateInventoryItems(List<InventoryItemDTO> items) {
        logger.info("Processing updateInventoryItems for {} items", items.size());
        List<InventoryItem> entitiesToUpdate = new ArrayList<>();
        List<String> updatedIds = new ArrayList<>();

        for (InventoryItemDTO dto : items) {
            InventoryItem existingItem = inventoryItemRepository.findById(dto.getItemId())
                    .orElseThrow(() -> {
                        logger.warn("Skipping updateInventoryItems - Item {} not found", dto.getItemId());
                        return new ResourceNotFoundException("Inventory Item not found with ID: " + dto.getItemId());
                    });

            // Conditional update for itemName
            if (dto.getItemName() != null && !dto.getItemName().trim().isEmpty()) {
                existingItem.setItemName(dto.getItemName());
            }

            // Always update availableStock
            existingItem.setAvailableStock(dto.getAvailableStock());

            entitiesToUpdate.add(existingItem);
            updatedIds.add(dto.getItemId());
        }

        inventoryItemRepository.saveAll(entitiesToUpdate);
        logger.info("updateInventoryItems completed successfully for {} items", items.size());
        return updatedIds;
    }

    @Override
    public void deleteInventoryItems(List<String> itemIds) {
        logger.info("Processing deleteInventoryItems for IDs: {}", itemIds);

        for (String id : itemIds) {
            if (!inventoryItemRepository.existsById(id)) {
                logger.warn("Skipping deleteInventoryItems - Item {} not found", id);
                throw new ResourceNotFoundException("Inventory Item not found with ID: " + id);
            }
        }

        inventoryItemRepository.deleteAllById(itemIds);
        logger.info("deleteInventoryItems completed successfully for {} items", itemIds.size());
    }

    @Override
    public List<String> addStock(List<AddStockRequestDTO> items) {
        logger.info("Processing addStock for {} items", items.size());
        List<InventoryItem> entitiesToUpdate = new ArrayList<>();
        List<String> updatedIds = new ArrayList<>();

        for (AddStockRequestDTO dto : items) {
            InventoryItem existingItem = inventoryItemRepository.findById(dto.getItemId())
                    .orElseThrow(() -> {
                        logger.warn("Skipping addStock - Item {} not found", dto.getItemId());
                        return new ResourceNotFoundException("Inventory Item not found with ID: " + dto.getItemId());
                    });

            existingItem.setAvailableStock(existingItem.getAvailableStock() + dto.getAddStock());

            entitiesToUpdate.add(existingItem);
            updatedIds.add(dto.getItemId());
        }

        inventoryItemRepository.saveAll(entitiesToUpdate);
        logger.info("addStock completed successfully for {} items", items.size());
        return updatedIds;
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
        item.setReservedStock(dto.getReservedStock() != null ? dto.getReservedStock() : 0);
        return item;
    }
}
