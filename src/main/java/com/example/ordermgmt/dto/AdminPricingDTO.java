package com.example.ordermgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin pricing dto")
public class AdminPricingDTO {
    @NotBlank(message = "Item ID is required")
    private String itemId;

    @NotNull(message = "Unit price is required")
    @PositiveOrZero(message = "Unit price must be non-negative")
    private BigDecimal unitPrice;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveFrom;
}
