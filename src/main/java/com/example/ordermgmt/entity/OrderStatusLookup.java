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
@Table(name = "ORDER_STATUS_LOOKUP", schema = "ordermgmt")
public class OrderStatusLookup {

    @Id
    @Column(name = "statusid")
    private Integer statusId;

    @Column(name = "statusname", nullable = false, length = 50, unique = true)
    private String statusName;
}
