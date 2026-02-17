package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "EMAIL_LOG", schema = "ordermgmt")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    private String subject;

    @Column(nullable = false)
    private String status;

    @CreatedBy
    @Column(name = "createdby", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "sentat", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(length = 1000)
    private String errorMessage;
}
