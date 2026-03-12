package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PRICING_HISTORY")
@EntityListeners(AuditingEntityListener.class)
public class PricingHistory {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "historyid", updatable = false, nullable = false)
    private UUID historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemid", referencedColumnName = "itemid", nullable = false, foreignKey = @ForeignKey(name = "FK_HISTORY_ITEM"))
    private InventoryItem inventoryItem;

    @Column(name = "oldprice", precision = 19, scale = 4)
    private BigDecimal oldPrice;

    @Column(name = "newprice", nullable = false, precision = 19, scale = 4)
    private BigDecimal newPrice;

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
