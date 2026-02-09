package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "REFRESH_TOKEN", schema = "ordermgmt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @Column(name = "tokenid", length = 36)
    private String tokenId;

    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private AppUser appUser;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expirydate", nullable = false)
    private Instant expiryDate;

    @Column(name = "revoked")
    private Boolean revoked = false;
}
