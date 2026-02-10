package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByAppUser(AppUser appUser);

    Optional<Customer> findByAppUserEmail(String email);
}
