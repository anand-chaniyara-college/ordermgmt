package com.example.ordermgmt.service.impl;

import com.example.ordermgmt.dto.analytics.MonthlySalesLogDTO;
import com.example.ordermgmt.repository.OrderItemRepository;
import com.example.ordermgmt.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Month;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final OrderItemRepository orderItemRepository;

    @Override
    public MonthlySalesLogDTO getMonthlyReport(String month, int year) {
        int monthInt;
        try {
            monthInt = Month.valueOf(month.toUpperCase()).getValue();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid month name: " + month);
        }

        MonthlySalesLogDTO report = orderItemRepository.getMonthlyReport(monthInt, year);
        if (report == null || report.getTotalSoldItems() == null || report.getTotalSoldItems() == 0) {
            return null;
        }
        return report;
    }
}
