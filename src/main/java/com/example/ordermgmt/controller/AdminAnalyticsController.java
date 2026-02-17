package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.analytics.MonthlyReportRequestDTO;
import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@Validated
@Tag(name = "7. Analytics & Reports (Admin)", description = "View sales trends, monthly performance, and send reports to email")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/monthlyreport")
    @Operation(summary = "View Monthly Sales Report", description = "Get a detailed breakdown of sales, quantities, and totals for a specific month and year")
    public ResponseEntity<MonthlySalesLogDTO> getMonthlyReport(
            @RequestParam @NotBlank(message = "Month is required") String month,
            @RequestParam @Min(value = 2000, message = "Year must be 2000 or later") Integer year) {

        MonthlySalesLogDTO report = adminAnalyticsService.getMonthlyReport(month, year);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/sendreportemail")
    @Operation(summary = "Email Sales Report", description = "Generate the monthly sales report and send it directly to the admin's email address")
    public ResponseEntity<String> sendReportEmail(
            @RequestBody @Valid MonthlyReportRequestDTO request,
            Principal principal) {

        String adminEmail = principal.getName();
        adminAnalyticsService.sendMonthlyReportEmail(request.getMonth(), request.getYear(), adminEmail);
        return ResponseEntity.ok("Report email request submitted for " + adminEmail);
    }
}
