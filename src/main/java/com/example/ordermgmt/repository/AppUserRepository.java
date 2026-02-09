package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByEmail(String email);
}
