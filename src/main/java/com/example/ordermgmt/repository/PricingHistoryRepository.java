package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.PricingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingHistoryRepository extends JpaRepository<PricingHistory, String> {

    List<PricingHistory> findAllByInventoryItemItemIdOrderByCreatedTimestampDesc(String itemId);

    Optional<PricingHistory> findFirstByInventoryItemItemIdOrderByCreatedTimestampDesc(String itemId);
}
