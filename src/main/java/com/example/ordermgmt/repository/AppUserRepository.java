package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// @Repository: Tells Spring "This interface handles Database operations"
// We extend JpaRepository to get free methods like save(), findAll(), findById()
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, String> {

    // Custom finder method
    // Spring Boot automatically implements this based on the method name!
    // "findByEmail" -> SQL: SELECT * FROM app_user WHERE email = ?
    Optional<AppUser> findByEmail(String email);
}
