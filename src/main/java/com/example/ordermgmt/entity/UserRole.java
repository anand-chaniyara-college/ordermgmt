package com.example.ordermgmt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
// @Entity: This class maps to a database table
@Entity
// @Table: Maps to the "USER_ROLE" table in database
@Table(name = "USER_ROLE", schema = "ordermgmt")
public class UserRole {

    // Simple Primary Key
    @Id
    @Column(name = "roleid")
    private Integer roleId;

    // The name of the role (e.g., "CUSTOMER", "ADMIN")
    @Column(name = "rolename", nullable = false, length = 50, unique = true)
    private String roleName;
}
