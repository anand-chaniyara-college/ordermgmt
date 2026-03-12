package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "EMAIL_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EmailLog {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "subject")
    private String subject;

    @Column(name = "status", nullable = false)
    private String status;

    @TenantId
    @Column(name = "org_id")
    private UUID orgId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", insertable = false, updatable = false)
    private Organization org;

    @CreatedBy
    @Column(name = "createdby", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "sentat", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "errormessage", length = 1000)
    private String errorMessage;
}
