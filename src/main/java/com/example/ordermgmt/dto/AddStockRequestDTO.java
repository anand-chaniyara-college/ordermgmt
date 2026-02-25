package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Add stock request dto")
public class AddStockRequestDTO {

    @NotNull(message = "Item ID is required")
    private UUID itemId;

    @NotNull(message = "Add Stock amount is required")
    @Min(value = 1, message = "Add Stock amount must be greater than zero")
    private Integer addStock;
}
