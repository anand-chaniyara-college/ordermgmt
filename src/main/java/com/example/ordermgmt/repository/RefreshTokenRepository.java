package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.AppUser;
import com.example.ordermgmt.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    // Find a token object by the string token
    Optional<RefreshToken> findByToken(String token);

    // Find all tokens for a user (useful if you want to revoke all sessions)
    // List<RefreshToken> findByAppUser(AppUser appUser);
}
