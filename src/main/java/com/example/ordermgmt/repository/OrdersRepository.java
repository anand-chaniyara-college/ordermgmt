package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, String> {
    List<Orders> findByCustomerAppUserEmail(String email);

    List<Orders> findByCustomerCustomerId(String customerId);
}
