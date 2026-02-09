package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.AdminPricingDTO;
import java.util.List;

public interface AdminPriceService {
    List<AdminPricingDTO> getAllPrices();

    List<AdminPricingDTO> getPriceHistory(String itemId);

    String addPrice(AdminPricingDTO pricingDTO);

    String updatePrice(AdminPricingDTO pricingDTO);
}
