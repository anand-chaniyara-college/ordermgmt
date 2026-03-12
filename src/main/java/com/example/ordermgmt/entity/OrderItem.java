package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.hibernate.annotations.TenantId;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ORDER_ITEM")
@EntityListeners(AuditingEntityListener.class)
public class OrderItem {

    @EmbeddedId
    private OrderItemId id;

    @MapsId("orderId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderid", nullable = false)
    private Orders order;

    @MapsId("itemId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemid", referencedColumnName = "itemid", nullable = false, foreignKey = @ForeignKey(name = "FK_ORDERITEM_ITEM", foreignKeyDefinition = "FOREIGN KEY (itemid) REFERENCES ordermgmt.INVENTORY_ITEM(itemid) ON UPDATE CASCADE"))
    private InventoryItem inventoryItem;

    @Positive(message = "Quantity must be positive")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @PositiveOrZero(message = "Unit price must be non-negative")
    @Column(name = "unitprice", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class OrderItemId implements Serializable {

        @Column(name = "orderid", updatable = false, nullable = false)
        private UUID orderId;

        @Column(name = "itemid", updatable = false, nullable = false)
        private UUID itemId;
    }
}
