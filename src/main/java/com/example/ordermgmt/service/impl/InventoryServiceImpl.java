package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.InventoryItemDTO;
import com.example.ordermgmt.entity.InventoryItem;
import com.example.ordermgmt.repository.InventoryItemRepository;
import com.example.ordermgmt.service.InventoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepository inventoryItemRepository;

    public InventoryServiceImpl(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public List<InventoryItemDTO> getAllInventory() {
        System.out.println("  >>> [InventoryServiceImpl] Fetching all inventory items...");

        // 1. Fetch all 'Entity' objects from Database
        List<InventoryItem> entities = inventoryItemRepository.findAll();

        // 2. Convert them to 'DTO' objects for the API response
        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public String addInventoryItem(InventoryItemDTO itemDTO) {
        System.out.println("  >>> [InventoryServiceImpl] Adding new inventory item: " + itemDTO.getItemId());

        // 1. Check if ID already exists to prevent overwriting
        if (inventoryItemRepository.existsById(itemDTO.getItemId())) {
            return "Item ID already exists";
        }

        // 2. Convert DTO (API Data) to Entity (DB Data)
        InventoryItem item = convertToEntity(itemDTO);

        // 3. Save to Database
        inventoryItemRepository.save(item);

        return "Item added successfully";
    }

    @Override
    public String updateInventoryItem(String itemId, InventoryItemDTO itemDTO) {
        System.out.println("  >>> [InventoryServiceImpl] Updating inventory item: " + itemId);

        // 1. Find the existing item
        // .map() here works like "If Found, do this..."
        return inventoryItemRepository.findById(itemId).map(existingItem -> {

            // 2. Update the fields
            existingItem.setAvailableStock(itemDTO.getAvailableStock());
            existingItem.setReservedStock(itemDTO.getReservedStock());

            // 3. Save changes back to Database
            inventoryItemRepository.save(existingItem);

            return "Item updated successfully";

        }).orElse("Item not found"); // If NOT found
    }

    @Override
    public String deleteInventoryItem(String itemId) {
        System.out.println("  >>> [InventoryServiceImpl] Deleting inventory item: " + itemId);

        // 1. Check existence
        if (inventoryItemRepository.existsById(itemId)) {
            // 2. Delete
            inventoryItemRepository.deleteById(itemId);
            return "Item deleted successfully";
        } else {
            return "Item not found";
        }
    }

    // Helper methods to convert between DTO and Entity
    private InventoryItemDTO convertToDTO(InventoryItem item) {
        return new InventoryItemDTO(item.getItemId(), item.getAvailableStock(), item.getReservedStock());
    }

    private InventoryItem convertToEntity(InventoryItemDTO dto) {
        return new InventoryItem(dto.getItemId(), dto.getAvailableStock(), dto.getReservedStock());
    }
}
