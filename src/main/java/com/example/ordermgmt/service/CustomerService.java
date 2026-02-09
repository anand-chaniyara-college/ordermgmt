package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.CustomerProfileDTO;

public interface CustomerService {
    CustomerProfileDTO getCustomerProfile(String email);

    String updateCustomerProfile(String email, CustomerProfileDTO profileDTO);
}
