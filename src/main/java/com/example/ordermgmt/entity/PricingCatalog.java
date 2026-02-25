package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.CreatedBy;
import jakarta.validation.constraints.PositiveOrZero;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PRICING_CATALOG", schema = "ordermgmt")
@EntityListeners(AuditingEntityListener.class)
public class PricingCatalog {

    @Id
    @Column(name = "itemid", updatable = false, nullable = false)
    private UUID itemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "itemid", referencedColumnName = "itemid", foreignKey = @ForeignKey(name = "FK_PRICING_ITEM"))
    private InventoryItem inventoryItem;

    @PositiveOrZero(message = "Unit price must be non-negative")
    @Column(name = "unitprice", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @CreatedDate
    @Column(name = "createdtimestamp", nullable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "updatedtimestamp")
    private LocalDateTime updatedTimestamp;

    @CreatedBy
    @Column(name = "createdby", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updatedby")
    private String updatedBy;
}
