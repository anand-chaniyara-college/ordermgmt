package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
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
@Table(name = "INVENTORY_ITEM")
@EntityListeners(AuditingEntityListener.class)
public class InventoryItem {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "itemid", updatable = false, nullable = false)
    private UUID itemId;

    @Column(name = "itemname", length = 100)
    private String itemName;

    @Min(value = 0, message = "Available stock must be non-negative")
    @Column(name = "availablestock", nullable = false)
    private Integer availableStock;

    @Min(value = 0, message = "Reserved stock must be non-negative")
    @Column(name = "reservedstock", nullable = false)
    private Integer reservedStock;

    @OneToOne(mappedBy = "inventoryItem", cascade = CascadeType.ALL)
    private PricingCatalog pricingCatalog;

    @OneToMany(mappedBy = "inventoryItem", cascade = CascadeType.PERSIST)
    private List<PricingHistory> pricingHistoryLogs;

    @Version
    @Column(name = "version")
    private Long version;

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
