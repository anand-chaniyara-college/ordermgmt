package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.PricingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PricingHistoryRepository extends JpaRepository<PricingHistory, UUID> {

    Optional<PricingHistory> findFirstByInventoryItemItemIdOrderByCreatedTimestampDesc(UUID itemId);
}
