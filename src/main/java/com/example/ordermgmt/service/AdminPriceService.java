package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.AdminPricingDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AdminPriceService {
    List<AdminPricingDTO> getAllPrices();

    Page<AdminPricingDTO> getAllPrices(Pageable pageable);

    AdminPricingDTO getPrice(UUID itemId);

    void addPrices(List<AdminPricingDTO> prices);

    void updatePrices(List<AdminPricingDTO> prices);
}
