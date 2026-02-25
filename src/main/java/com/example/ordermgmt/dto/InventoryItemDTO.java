package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Inventory item dto")
public class InventoryItemDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "System-generated UUID. Omit on creation; required on update.")
    private UUID itemId;

    @Pattern(regexp = "^[a-zA-Z0-9 \\-_.]+$", message = "Item Name must contain only alphanumeric characters, spaces, hyphens, underscores, or dots")
    @Size(max = 100, message = "Item Name must not exceed 100 characters")
    private String itemName;

    @NotNull(message = "Available Stock cannot be null")
    @Min(value = 0, message = "Available Stock must be zero or positive")
    private Integer availableStock;

    @Min(value = 0, message = "Reserved Stock must be zero or positive")
    private Integer reservedStock;
}
