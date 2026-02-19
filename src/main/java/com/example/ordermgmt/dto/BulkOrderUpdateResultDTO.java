package com.example.ordermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkOrderUpdateResultDTO {
    private List<OrderDTO> successes;
    private List<BulkOrderFailureDTO> failures;
}
