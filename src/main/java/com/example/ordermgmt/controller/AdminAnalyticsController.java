package com.example.ordermgmt.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@Validated
@Tag(name = "7. Analytics & Reports (Admin)", description = "View sales trends and monthly performance")
public class AdminAnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsController.class);
    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/monthlyreport")
    @Operation(summary = "View Monthly Sales Report", description = "Get a detailed breakdown of sales, quantities, and totals for a specific month and year")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report retrieved successfully", content = @Content(schema = @Schema(implementation = MonthlySalesLogDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid month or year parameter", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<MonthlySalesLogDTO> getMonthlyReport(
            @RequestParam @NotBlank(message = "Month is required") String month,
            @RequestParam @Min(value = 2000, message = "Year must be 2000 or later") Integer year) {

        logger.info("Processing getMonthlyReport for: {}-{}", month, year);
        MonthlySalesLogDTO report = adminAnalyticsService.getMonthlyReport(month, year);
        logger.info("getMonthlyReport completed successfully for: {}-{}", month, year);
        return ResponseEntity.ok(report);
    }
}
