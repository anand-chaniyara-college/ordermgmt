package com.example.ordermgmt.repository;

import com.example.ordermgmt.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByOrgIdAndEmailIgnoreCase(UUID orgId, String email);

    boolean existsByOrgIdAndEmailIgnoreCase(UUID orgId, String email);

    List<AppUser> findByRole_RoleName(String roleName);

    List<AppUser> findByRole_RoleNameAndOrgId(String roleName, UUID orgId);
}
