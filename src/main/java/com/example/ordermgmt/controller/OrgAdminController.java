package com.example.ordermgmt.controller;

import com.example.ordermgmt.dto.CreateAdminRequestDTO;
import com.example.ordermgmt.dto.UpdateUserStatusRequestDTO;
import com.example.ordermgmt.dto.UserResponseDTO;
import com.example.ordermgmt.dto.analytics.MonthlyReportRequestDTO;
import com.example.ordermgmt.dto.analytics.OrderAnalyticsResponseDTO;
import com.example.ordermgmt.dto.analytics.RevenueReportResponseDTO;
import com.example.ordermgmt.exception.InvalidOperationException;
import com.example.ordermgmt.service.AdminAnalyticsService;
import com.example.ordermgmt.service.OrgAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/org-admin")
@Validated
public class OrgAdminController {

    private static final Logger logger = LoggerFactory.getLogger(OrgAdminController.class);

    private final OrgAdminService orgAdminService;
    private final AdminAnalyticsService adminAnalyticsService;

    public OrgAdminController(OrgAdminService orgAdminService, AdminAnalyticsService adminAnalyticsService) {
        this.orgAdminService = orgAdminService;
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @PostMapping("/admins")
    public ResponseEntity<UserResponseDTO> createAdmin(
            @Valid @RequestBody CreateAdminRequestDTO request,
            Principal principal) {
        logger.info("Processing createAdmin for OrgAdmin: {}", principal.getName());
        UserResponseDTO response = orgAdminService.createAdmin(principal.getName(), request);
        logger.info("createAdmin completed successfully for OrgAdmin: {}", principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/admins")
    public ResponseEntity<Map<String, List<UserResponseDTO>>> listAdmins(Principal principal) {
        logger.info("Processing listAdmins for OrgAdmin: {}", principal.getName());
        List<UserResponseDTO> admins = orgAdminService.listAdmins(principal.getName());
        logger.info("listAdmins completed successfully for OrgAdmin: {}", principal.getName());
        return ResponseEntity.ok(Map.of("admins", admins));
    }

    @PatchMapping("/admins/{id}/status")
    public ResponseEntity<Map<String, String>> updateAdminStatus(
            @PathVariable("id") UUID adminUserId,
            @Valid @RequestBody UpdateUserStatusRequestDTO request,
            Principal principal) {
        logger.info("Processing updateAdminStatus for User: {}", adminUserId);
        orgAdminService.updateAdminStatus(principal.getName(), adminUserId, request);
        logger.info("updateAdminStatus completed successfully for User: {}", adminUserId);
        return ResponseEntity.ok(Map.of("message", "Admin status updated successfully."));
    }

    @PostMapping("/analytics/sendreportemail")
    public ResponseEntity<Map<String, String>> sendReportEmail(
            @RequestBody @Valid MonthlyReportRequestDTO request,
            Principal principal) {
        String orgAdminEmail = principal.getName();
        logger.info("Processing sendReportEmail for OrgAdmin: {}", orgAdminEmail);
        adminAnalyticsService.sendMonthlyReportEmail(request.getMonth(), request.getYear(), orgAdminEmail);
        logger.info("sendReportEmail completed successfully for OrgAdmin: {}", orgAdminEmail);
        return ResponseEntity.ok(Map.of("message", "Report email request submitted for " + orgAdminEmail));
    }

    @GetMapping("/analytics/revenue-report")
    public ResponseEntity<RevenueReportResponseDTO> getRevenueReport(
            @RequestParam("startdate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("enddate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "itemname", required = false) String itemName,
            @io.swagger.v3.oas.annotations.Parameter(description = "Page number (0-indexed)") @RequestParam(required = false) Integer page,
            @io.swagger.v3.oas.annotations.Parameter(description = "Page size") @RequestParam(required = false) Integer size) {

        if (page != null && size != null) {
            logger.info("Processing getRevenueReport (Page) for range: {} to {}", startDate, endDate);
            Pageable pageable = PageRequest.of(page, size);
            RevenueReportResponseDTO report = adminAnalyticsService.getRevenueReport(startDate, endDate, itemName,
                    pageable);
            logger.info("getRevenueReport (Page) completed successfully for range: {} to {}", startDate, endDate);
            return ResponseEntity.ok(report);
        }

        logger.info("Processing getRevenueReport for range: {} to {}", startDate, endDate);
        RevenueReportResponseDTO report = adminAnalyticsService.getRevenueReport(startDate, endDate, itemName, null);
        logger.info("getRevenueReport completed successfully for range: {} to {}", startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/analytics/order-analytics")
    public ResponseEntity<OrderAnalyticsResponseDTO> getOrderAnalytics(
            @RequestParam("startdate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("enddate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "itemname", required = false) String itemName,
            @RequestParam(value = "orderstatus", required = false) String orderStatus,
            @io.swagger.v3.oas.annotations.Parameter(description = "Page number (0-indexed)") @RequestParam(required = false) Integer page,
            @io.swagger.v3.oas.annotations.Parameter(description = "Page size") @RequestParam(required = false) Integer size) {

        if (page != null && size != null) {
            logger.info("Processing getOrderAnalytics (Page) for range: {} to {}", startDate, endDate);
            Pageable pageable = PageRequest.of(page, size);
            OrderAnalyticsResponseDTO report = adminAnalyticsService.getOrderAnalytics(startDate, endDate, itemName,
                    orderStatus, pageable);
            logger.info("getOrderAnalytics (Page) completed successfully for range: {} to {}", startDate, endDate);
            return ResponseEntity.ok(report);
        }

        logger.info("Processing getOrderAnalytics for range: {} to {}", startDate, endDate);
        OrderAnalyticsResponseDTO report = adminAnalyticsService.getOrderAnalytics(startDate, endDate, itemName,
                orderStatus, null);
        logger.info("getOrderAnalytics completed successfully for range: {} to {}", startDate, endDate);
        return ResponseEntity.ok(report);
    }
}
