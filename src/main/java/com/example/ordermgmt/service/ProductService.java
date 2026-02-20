package com.example.ordermgmt.service;

import com.example.ordermgmt.dto.ProductDTO;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    List<ProductDTO> getAvailableProducts();

    Page<ProductDTO> getAvailableProducts(Pageable pageable);
}
