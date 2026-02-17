package com.example.ordermgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private String orderId;
    private String customerId;
    private String status;
    private LocalDateTime createdTimestamp;
    private LocalDateTime updatedTimestamp;

    @Valid
    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemDTO> items;

    private BigDecimal totalAmount;
}
