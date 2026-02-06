package com.example.ordermgmt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "INVENTORY_ITEM", schema = "ordermgmt")
public class InventoryItem {

    @Id
    @Column(name = "itemid", length = 50)
    private String itemId;

    @Min(value = 0, message = "Available stock must be non-negative")
    @Column(name = "availablestock", nullable = false)
    private Integer availableStock;

    @Min(value = 0, message = "Reserved stock must be non-negative")
    @Column(name = "reservedstock", nullable = false)
    private Integer reservedStock;

    @AssertTrue(message = "Available stock must be greater than or equal to reserved stock")
    public boolean isStockConsistent() {
        if (availableStock == null || reservedStock == null) {
            return true;
        }
        return availableStock >= reservedStock;
    }
}
