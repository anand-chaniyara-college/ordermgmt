package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ORDERS", schema = "ordermgmt")
public class Orders {

    @Id
    @Column(name = "orderid", length = 36)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerid", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statusid", nullable = false)
    private OrderStatusLookup status;

    @Column(name = "createdtimestamp", nullable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "updatedtimestamp", nullable = false)
    private LocalDateTime updatedTimestamp;
}
