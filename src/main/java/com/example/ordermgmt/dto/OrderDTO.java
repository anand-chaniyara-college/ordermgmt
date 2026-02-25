package com.example.ordermgmt.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order dto")
public class OrderDTO {
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private UUID orderId;

    @JsonIgnore
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private UUID customerId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String status;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdTimestamp;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedTimestamp;

    @Valid
    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemDTO> items;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal totalAmount;
}
