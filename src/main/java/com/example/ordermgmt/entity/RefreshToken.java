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
@Table(name = "REFRESH_TOKEN", schema = "ordermgmt")
public class RefreshToken {

    @Id
    @Column(name = "tokenid", length = 36)
    private String tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userid", nullable = false)
    private AppUser appUser;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expirydate", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "revoked")
    private Boolean revoked = false;
}
