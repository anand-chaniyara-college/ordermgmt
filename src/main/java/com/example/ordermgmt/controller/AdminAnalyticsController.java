package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/monthlyreport")
    public ResponseEntity<?> getMonthlyReport(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer year) {

        if (month == null || year == null) {
            return ResponseEntity.badRequest().body(
                    "Error: Missing required parameters. Please provide 'month' (e.g., January) and 'year' (e.g., 2024).");
        }

        try {
            MonthlySalesLogDTO report = adminAnalyticsService.getMonthlyReport(month, year);
            if (report == null) {
                return ResponseEntity.status(404).body("No records found for " + month + " " + year);
            }
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @PostMapping("/sendreportemail")
    public ResponseEntity<String> sendReportEmail(
            @RequestBody com.example.ordermgmt.dto.analytics.MonthlyReportRequestDTO request,
            java.security.Principal principal) {

        String adminEmail = principal.getName();
        try {
            adminAnalyticsService.sendMonthlyReportEmail(request.getMonth(), request.getYear(), adminEmail);
            return ResponseEntity.ok("Report email request submitted for " + adminEmail);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An unexpected error occurred: " + e.getMessage());
        }
    }
}
