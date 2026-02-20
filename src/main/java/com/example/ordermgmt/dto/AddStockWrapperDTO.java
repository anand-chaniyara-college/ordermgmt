package com.example.ordermgmt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddStockWrapperDTO {

    @Valid
    @NotEmpty(message = "Add stock list must not be empty")
    private List<AddStockRequestDTO> addstock;
}
