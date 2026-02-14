package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.annotations.GenericGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PRICING_CATALOG", schema = "ordermgmt")
public class PricingCatalog {

    @Id
    @Column(name = "itemid", length = 50)
    private String itemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "itemid", referencedColumnName = "itemid", foreignKey = @ForeignKey(name = "FK_PRICING_ITEM"))
    private InventoryItem inventoryItem;

    @PositiveOrZero(message = "Unit price must be non-negative")
    @Column(name = "unitprice", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "updatedtimestamp", nullable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime updatedTimestamp;
}
