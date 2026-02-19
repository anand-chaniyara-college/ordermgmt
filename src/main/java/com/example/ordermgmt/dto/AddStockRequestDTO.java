package com.example.ordermgmt.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddStockRequestDTO {

    @NotBlank(message = "Item ID is required")
    private String itemId;

    @NotNull(message = "Add Stock amount is required")
    @Min(value = 1, message = "Add Stock amount must be greater than zero")
    private Integer addStock;
}
