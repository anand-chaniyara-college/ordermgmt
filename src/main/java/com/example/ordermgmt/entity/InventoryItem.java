package com.example.ordermgmt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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

    @Column(name = "availablestock", nullable = false)
    private Integer availableStock;

    @Column(name = "reservedstock", nullable = false)
    private Integer reservedStock;
}
