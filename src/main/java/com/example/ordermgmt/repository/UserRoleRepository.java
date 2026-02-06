package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

    // Finds a Role by its name (e.g., "CUSTOMER")
    // Returns Optional because the role might not exist in the DB
    Optional<UserRole> findByRoleName(String roleName);
}
