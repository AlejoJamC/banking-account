package com.waes.rabobank.bankingaccount.domain.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_bsn", columnList = "bsn_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class) // Review Automatic Auditing
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // java 21 uuid native support
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "bsn_id", unique = true)
    private String bsnId; // Dutch BSN (citizen service number, BurgerServiceNummer)

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Account> accounts = new ArrayList<>();

    @CreatedDate // Review spring Data 3.x enhanced auditing
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    // Java 21: review compact constructors
    protected User() {
        // JPA required
    }

    public User(String email, String fullName, String bsnId) {
        this.email = email;
        this.fullName = fullName;
        this.bsnId = bsnId;
    }

    // Getters
    public User(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getBsnId() {
        return bsnId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setEmail(String email) {
        this.email = email;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
