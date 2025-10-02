package com.waes.rabobank.bankingaccount.domain.model;

import com.waes.rabobank.bankingaccount.domain.enums.CardStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // review single table and discriminator column
@DiscriminatorColumn(name = "card_type", discriminatorType = DiscriminatorType.STRING)
@EntityListeners(AuditingEntityListener.class)
public sealed abstract class Card permits DebitCard, CreditCard { // review java 21 sealed classes

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "card_number", nullable = false, unique = true, length = 16)
    private String cardNumber;

    @Column(name = "expiry_date", nullable = false, length = 5)
    private YearMonth expiryDate; // MM/YY format

    @Column(name = "ccv_encrypted", nullable = false, length = 100)
    private String ccvEncrypted; // Review encryption at rest

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status = CardStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Card() {
    }

    public Card(Account account, String cardNumber, YearMonth expiryDate) {
        this.account = account;
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
    }

    // Review business logic in entity
    public abstract BigDecimal calculateFee(BigDecimal amount);

    public boolean isExpired() {
        return YearMonth.now().isAfter(expiryDate);
    }

    public boolean isActive() {
        return status == CardStatus.ACTIVE && !isExpired();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public YearMonth getExpiryDate() {
        return expiryDate;
    }

    public String getCcvEncrypted() {
        return ccvEncrypted;
    }

    public CardStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }
}
