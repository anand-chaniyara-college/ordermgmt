package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PRICING_CATALOG", schema = "ordermgmt")
public class PricingCatalog {

    @EmbeddedId
    private PricingCatalogId id;

    @MapsId("itemId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemid", referencedColumnName = "itemid")
    private InventoryItem inventoryItem;

    @Column(name = "unitprice", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class PricingCatalogId implements Serializable {
        @Column(name = "itemid", length = 50)
        private String itemId;

        @Column(name = "createdtimestamp")
        private LocalDateTime createdTimestamp;
    }
}
