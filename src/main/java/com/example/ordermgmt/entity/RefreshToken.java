package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

@Entity
@Table(name = "REFRESH_TOKEN", schema = "ordermgmt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
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

    @CreatedDate
    @Column(name = "createdtimestamp", nullable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @LastModifiedDate
    @Column(name = "updatedtimestamp")
    private LocalDateTime updatedTimestamp;

    @CreatedBy
    @Column(name = "createdby", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updatedby")
    private String updatedBy;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RefreshToken that = (RefreshToken) o;
        return tokenId != null && tokenId.equals(that.tokenId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
