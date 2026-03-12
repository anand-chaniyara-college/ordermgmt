package com.example.ordermgmt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ORDER_STATUS_LOOKUP")
@EntityListeners(AuditingEntityListener.class)
public class OrderStatusLookup {

    @Id
    @Column(name = "statusid")
    private Integer statusId;

    @Column(name = "statusname", nullable = false, length = 50, unique = true)
    private String statusName;

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
