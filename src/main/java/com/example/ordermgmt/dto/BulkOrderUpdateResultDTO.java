package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk order update result dto")
public class BulkOrderUpdateResultDTO {
    private List<OrderDTO> successes;
    private List<BulkOrderFailureDTO> failures;
}
