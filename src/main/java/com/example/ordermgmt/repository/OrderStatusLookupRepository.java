package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.OrderStatusLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrderStatusLookupRepository extends JpaRepository<OrderStatusLookup, Integer> {
    Optional<OrderStatusLookup> findByStatusName(String statusName);
}
