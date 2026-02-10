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
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "uuid", length = 36)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemid", referencedColumnName = "itemid", nullable = false, foreignKey = @ForeignKey(name = "FK_PRICING_ITEM", foreignKeyDefinition = "FOREIGN KEY (itemid) REFERENCES ordermgmt.INVENTORY_ITEM(itemid) ON UPDATE CASCADE"))
    private InventoryItem inventoryItem;

    @PositiveOrZero(message = "Unit price must be non-negative")
    @Column(name = "unitprice", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "createdtimestamp", nullable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime createdTimestamp;
}
