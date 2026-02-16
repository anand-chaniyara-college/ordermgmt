package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.PricingCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PricingCatalogRepository extends JpaRepository<PricingCatalog, String> {

}
