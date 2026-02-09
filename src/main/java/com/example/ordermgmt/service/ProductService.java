package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.ProductDTO;
import java.util.List;

public interface ProductService {
    List<ProductDTO> getAvailableProducts();
}
