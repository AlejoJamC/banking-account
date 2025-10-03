package com.waes.rabobank.bankingaccount.domain.model;

import com.waes.rabobank.bankingaccount.domain.enums.TransactionType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_account_created", columnList = "account_id, created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal fee = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_account_id")
    private Account relatedAccount; // For transfers, null otherwise

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_transaction_id")
    private Transaction relatedTransaction; // Link between deposit and withdrawal

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(length = 255) // At least this is the size used by my bank account
    private String description;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    protected Transaction() {
    }

    public Transaction(
            Account account,
            Card card,
            TransactionType type,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal balanceAfter
    ) {
        this.account = account;
        this.card = card;
        this.type = type;
        this.amount = amount;
        this.fee = fee;
        this.balanceAfter = balanceAfter;
    }

    // Review pragmatic location for Factory Methods - java 21
    public static Transaction withDrawal(
            Account account,
            Card card,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal balanceAfter
    ) {
        Transaction transaction = new Transaction(account, card, TransactionType.WITHDRAWAL, amount, fee, balanceAfter);

        return transaction;
    }

    public static Transaction transfer(
            Account account,
            Card card,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal balanceAfter,
            Account relatedAccount
    ) {
        var transaction = new Transaction(account, card, TransactionType.TRANSFER, amount, fee, balanceAfter);
        transaction.relatedAccount = relatedAccount;

        return transaction;
    }

    public static Transaction deposit(
            Account account,
            BigDecimal amount,
            BigDecimal balanceAfter,
            Transaction relatedTransaction
    ) {
        var transaction = new Transaction(account, relatedTransaction.getCard(), TransactionType.DEPOSIT, amount, BigDecimal.ZERO, balanceAfter);
        transaction.relatedTransaction = relatedTransaction;

        return transaction;
    }

    public BigDecimal getTotalAmount() {
        return amount.add(fee);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public Card getCard() {
        return card;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public Account getRelatedAccount() {
        return relatedAccount;
    }

    public Transaction getRelatedTransaction() {
        return relatedTransaction;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
