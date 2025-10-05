package com.waes.rabobank.bankingaccount.domain.model;

import com.waes.rabobank.bankingaccount.domain.enums.AccountStatus;
import com.waes.rabobank.bankingaccount.shared.exception.InsufficientFundsException;
import jakarta.persistence.*;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
@OptimisticLocking(type = OptimisticLockType.VERSION) // Protecting against race conditions - Review
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_number", nullable = false, unique = true, length = 34)
    private String accountNumber; // Review IBAN format

    @Column(nullable = false, precision = 19, scale = 4) // review setting precision and scale
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "EUR"; // Review ISO 4217

    @Enumerated(EnumType.STRING) // Review EnumType.ORDINAL vs STRING
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Version // Review Jakarta Persistence versioning
    private Long version;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private Card card;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    // Constructors
    protected Account() {
    }

    // Business constructor
    public Account(User user, String accountNumber) {
        this.user = user;
        this.accountNumber = accountNumber;
    }

    // Business methods
    // Review business rule
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    this.getId(),
                    this.balance,
                    amount
            );
        }
        this.balance = this.balance.subtract(amount);
    }

    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    public boolean hasCard() {
        return card != null;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public Card getCard() {
        return card;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setCard(Card card) {
        this.card = card;
        if (card != null && card.getAccount() != this) {
            card.setAccount(this);
        }
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
