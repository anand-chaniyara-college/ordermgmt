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

    // We link back to the User who owns this token
    @ManyToOne
    @JoinColumn(name = "userid", nullable = false)
    private AppUser appUser;

    // The actual secure random string
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    // When does this expire?
    @Column(name = "expirydate", nullable = false)
    private Instant expiryDate;

    // Has it been cancelled?
    @Column(name = "revoked")
    private Boolean revoked = false;
}
