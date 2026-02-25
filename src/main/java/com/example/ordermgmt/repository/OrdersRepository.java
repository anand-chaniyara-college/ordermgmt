package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, UUID> {
    List<Orders> findByCustomerAppUserEmail(String email);

    Page<Orders> findByCustomerAppUserEmail(String email, Pageable pageable);

    List<Orders> findByCustomerCustomerId(UUID customerId);

    @Query("SELECT o FROM Orders o WHERE o.status.statusName = :statusName AND o.createdTimestamp < :cutoff")
    List<Orders> findStalePendingOrders(@Param("statusName") String statusName,
            @Param("cutoff") LocalDateTime cutoff);
}
