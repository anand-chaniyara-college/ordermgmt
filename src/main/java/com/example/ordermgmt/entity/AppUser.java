package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "APP_USER", schema = "ordermgmt")
public class AppUser {

    @Id
    @Column(name = "userid", length = 36)
    private String userId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "passwordhash", nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roleid", nullable = false)
    private UserRole role;

    @Column(name = "isactive")
    private Boolean isActive = true;

    @Column(name = "createdtimestamp", nullable = false)
    private LocalDateTime createdTimestamp;
}
