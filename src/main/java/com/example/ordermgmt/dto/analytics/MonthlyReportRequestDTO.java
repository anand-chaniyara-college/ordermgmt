package com.example.ordermgmt.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Monthly report request dto")
public class MonthlyReportRequestDTO {

    @NotBlank(message = "Month is required")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Month must contain only alphabets")
    private String month;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be 2000 or later")
    private Integer year;
}
