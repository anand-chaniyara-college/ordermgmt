package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ORDERS")
@EntityListeners(AuditingEntityListener.class)
public class Orders {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "orderid", updatable = false, nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerid", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statusid", nullable = false)
    private OrderStatusLookup status;

    @TenantId
    @Column(name = "org_id")
    private UUID orgId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", insertable = false, updatable = false)
    private Organization org;

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
}
