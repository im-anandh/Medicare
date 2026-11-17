package com.medicare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a login account. Every person who can log in - admin,
 * doctor, receptionist, or patient - has exactly one row here.
 *
 * KEY ANNOTATIONS EXPLAINED (these apply to every entity in this project,
 * so we only explain them in detail once, here):
 *
 * @Entity
 *   Tells Hibernate "this class represents a database table." Without
 *   this, Hibernate ignores the class completely.
 *
 * @Table(name = "users")
 *   Sets the actual table name. Optional - if omitted, Hibernate would
 *   use the class name ("User") as the table name. We specify "users"
 *   explicitly to match the naming used throughout the project spec,
 *   and because "user" is a reserved word in some databases.
 *
 * @Id
 *   Marks the field that is this table's primary key.
 *
 * @GeneratedValue(strategy = GenerationType.IDENTITY)
 *   Tells the database to auto-increment this column (1, 2, 3, ...)
 *   every time a new row is inserted, instead of us picking IDs manually.
 *
 * @Column
 *   Customizes how a field maps to its column - e.g. making it required
 *   (nullable = false) or unique (unique = true). Fields without this
 *   annotation still become columns; @Column is only needed when you
 *   want non-default behavior.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // We will NEVER store a plain-text password. This field will hold
    // a bcrypt hash once we wire up Spring Security in Phase 6.
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    // @Enumerated(EnumType.STRING) stores the enum as readable text
    // ("ADMIN", "DOCTOR", ...) in the database column, instead of the
    // default (EnumType.ORDINAL), which stores it as a plain number
    // (0, 1, 2...). STRING is safer: if we ever reorder the enum
    // constants later, ORDINAL values would silently point to the
    // wrong role. STRING is immune to that.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ---------- Constructors ----------

    // JPA requires a no-argument constructor so Hibernate can create
    // instances internally (e.g. when loading rows from the database)
    // before populating the fields via reflection.
    public User() {
    }

    // ---------- Getters and Setters ----------
    // Plain Java classes used by JPA need getters/setters: Hibernate
    // uses them to read and write field values.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
