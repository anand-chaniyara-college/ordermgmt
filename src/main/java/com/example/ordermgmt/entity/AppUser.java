package com.example.ordermgmt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

// @Getter, @Setter: Lombok annotations to automatically generate Getters and Setters
// @NoArgsConstructor, @AllArgsConstructor: Generate constructors
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// @Entity: Tells Spring Data JPA that this class represents a table in our
// database
@Entity
// @Table: Specifies the name of the database table ("APP_USER")
@Table(name = "APP_USER", schema = "ordermgmt")
public class AppUser {

    // @Id: Marks this field as the Primary Key
    @Id
    @Column(name = "userid", length = 36)
    private String userId;

    // unique = true: No two users can have the same email
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // Stores the ENCRYPTED password, never plain text
    @Column(name = "passwordhash", nullable = false)
    private String passwordHash;

    // @ManyToOne: One Role can be assigned to MANY Users (e.g. Many users are
    // "CUSTOMER")
    // FetchType.LAZY: Don't load the Role data unless we specifically ask for it
    // (saves performance)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roleid", nullable = false)
    private UserRole role;

    @Column(name = "isactive")
    private Boolean isActive = true;

    @Column(name = "createdtimestamp", nullable = false)
    private LocalDateTime createdTimestamp;
}
