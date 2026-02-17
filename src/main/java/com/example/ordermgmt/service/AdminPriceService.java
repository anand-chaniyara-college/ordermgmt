package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.AdminPricingDTO;
import java.util.List;

public interface AdminPriceService {
    List<AdminPricingDTO> getAllPrices();

    AdminPricingDTO getPrice(String itemId);

    void addPrice(AdminPricingDTO pricingDTO);

    void updatePrice(AdminPricingDTO pricingDTO);
}
