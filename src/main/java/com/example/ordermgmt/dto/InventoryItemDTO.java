package com.example.ordermgmt.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemDTO {

    @NotBlank(message = "Item ID is required")
    @Size(min = 2, max = 50, message = "Item ID must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Item ID must contain only alphanumeric characters")
    private String itemId;

    @NotBlank(message = "Item Name is required")
    @Size(min = 2, max = 100, message = "Item Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Item Name must contain only alphanumeric characters and spaces")
    private String itemName;

    @NotNull(message = "Available Stock cannot be null")
    @Min(value = 0, message = "Available Stock must be zero or positive")
    private Integer availableStock;

    @NotNull(message = "Reserved Stock cannot be null")
    @Min(value = 0, message = "Reserved Stock must be zero or positive")
    private Integer reservedStock;
}
