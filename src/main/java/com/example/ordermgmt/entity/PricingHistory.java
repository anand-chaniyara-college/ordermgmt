package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PRICING_HISTORY", schema = "ordermgmt")
public class PricingHistory {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @Column(name = "historyid", length = 36)
    private String historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemid", referencedColumnName = "itemid", nullable = false, foreignKey = @ForeignKey(name = "FK_HISTORY_ITEM"))
    private InventoryItem inventoryItem;

    @Column(name = "oldprice", precision = 19, scale = 4)
    private BigDecimal oldPrice;

    @Column(name = "newprice", nullable = false, precision = 19, scale = 4)
    private BigDecimal newPrice;

    @Column(name = "createdtimestamp", nullable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime createdTimestamp;

    @Column(name = "changedby", length = 100)
    private String changedBy;
}
