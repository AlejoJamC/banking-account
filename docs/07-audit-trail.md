# Requirement: It should be able to audit transfers or withdrawals

## Audit Strategy

### Complete Transaction History
Every withdrawal and transfer creates permanent audit records in the `transactions` table.

**Audit principles:**
- Immutable records (no updates/deletes)
- Timestamps for all events
- Complete context (who, what, when, how much)
- Linked transactions for transfers

## Transaction Entity

### Core Fields
```java
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Card card;

    @Enumerated(EnumType.STRING)
    private TransactionType type;  // WITHDRAWAL, TRANSFER, DEPOSIT

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    private Account relatedAccount;  // For transfers

    @ManyToOne(fetch = FetchType.LAZY)
    private Transaction relatedTransaction;  // Link TRANSFER → DEPOSIT

    @Column(length = 255)
    private String description;

    @CreatedDate
    private Instant createdAt;
}
```

## Withdrawal Audit

### Single Transaction Record
```java
Transaction transaction = new Transaction(
    account,
    card,
    TransactionType.WITHDRAWAL,
    request.amount(),
    fee,
    account.getBalance()  // Balance after withdrawal
);
transactionRepository.save(transaction);
```

### Response Includes Transaction ID
```java
return new WithdrawalResponseDTO(
    transaction.getId().toString(),  // ← Audit reference
    account.getId().toString(),
    card.getId().toString(),
    request.amount(),
    fee,
    account.getBalance()
);
```

**Why return transaction ID?**
- Client can reference for disputes
- Enables transaction lookup/cancellation
- Provides receipt number

### Example Audit Record
```json
{
  "id": "tx-123...",
  "account_id": "acc-456...",
  "card_id": "card-789...",
  "type": "WITHDRAWAL",
  "amount": 100.00,
  "fee": 1.00,
  "balance_after": 899.00,
  "created_at": "2025-10-05T14:30:00Z"
}
```

## Transfer Audit

### Two Linked Transactions
```java
// 1. Transfer OUT (sender side)
Transaction transferOut = Transaction.transfer(
    fromAccount,
    card,
    amount,
    fee,
    fromAccount.getBalance(),
    toAccount  // Related account
);
transactionRepository.save(transferOut);

// 2. Deposit IN (receiver side)
Transaction transferIn = Transaction.deposit(
    toAccount,
    amount,
    toAccount.getBalance(),
    transferOut  // Related transaction
);
transactionRepository.save(transferIn);
```

### Factory Methods
```java
public static Transaction transfer(
    Account account,
    Card card,
    BigDecimal amount,
    BigDecimal fee,
    BigDecimal balanceAfter,
    Account relatedAccount
) {
    var transaction = new Transaction(
        account, card, TransactionType.TRANSFER, 
        amount, fee, balanceAfter
    );
    transaction.relatedAccount = relatedAccount;
    return transaction;
}

public static Transaction deposit(
    Account account,
    BigDecimal amount,
    BigDecimal balanceAfter,
    Transaction relatedTransaction
) {
    var transaction = new Transaction(
        account, 
        relatedTransaction.getCard(),  // Same card
        TransactionType.DEPOSIT, 
        amount, 
        BigDecimal.ZERO,  // No fee on deposit
        balanceAfter
    );
    transaction.relatedTransaction = relatedTransaction;
    return transaction;
}
```

**Why factory methods?**
- Encapsulate transaction creation logic
- Ensure all required fields set
- Type-safe (can't create invalid transactions)

### Example Transfer Audit Records
```json
// Transaction 1: TRANSFER OUT
{
  "id": "tx-out-1",
  "account_id": "acc-sender",
  "card_id": "card-123",
  "type": "TRANSFER",
  "amount": 100.00,
  "fee": 1.00,
  "balance_after": 799.00,
  "related_account_id": "acc-receiver",
  "created_at": "2025-10-05T14:30:00Z"
}

// Transaction 2: DEPOSIT IN
{
  "id": "tx-in-2",
  "account_id": "acc-receiver",
  "card_id": "card-123",
  "type": "DEPOSIT",
  "amount": 100.00,
  "fee": 0.00,
  "balance_after": 600.00,
  "related_transaction_id": "tx-out-1",
  "created_at": "2025-10-05T14:30:01Z"
}
```

## Audit Queries

### Account Transaction History
```sql
SELECT 
    id,
    transaction_type,
    amount,
    fee,
    balance_after,
    created_at
FROM transactions
WHERE account_id = ?
ORDER BY created_at DESC;
```

### Find Specific Transaction
```sql
SELECT * FROM transactions WHERE id = ?;
```

### Transfers Between Two Accounts
```sql
SELECT * FROM transactions 
WHERE (account_id = ? AND related_account_id = ?)
   OR (account_id = ? AND related_transaction_id IN 
       (SELECT id FROM transactions WHERE account_id = ?))
ORDER BY created_at DESC;
```

### Reconstruct Balance History
```sql
SELECT 
    created_at,
    transaction_type,
    CASE 
        WHEN transaction_type = 'WITHDRAWAL' THEN -amount - fee
        WHEN transaction_type = 'TRANSFER' THEN -amount - fee
        WHEN transaction_type = 'DEPOSIT' THEN amount
    END as balance_change,
    balance_after
FROM transactions
WHERE account_id = ?
ORDER BY created_at ASC;
```

### Daily Transaction Summary
```sql
SELECT 
    DATE(created_at) as date,
    transaction_type,
    COUNT(*) as count,
    SUM(amount) as total_amount,
    SUM(fee) as total_fees
FROM transactions
WHERE account_id = ?
GROUP BY DATE(created_at), transaction_type
ORDER BY date DESC;
```

## Design Decisions

### Decision: Immutable Transactions
No UPDATE or DELETE operations on transactions table.

**Rationale:**
- Audit trail must be tamper-proof
- Regulatory requirement (SOX, PCI-DSS)
- Transaction corrections done via new reversing entries

**Alternative considered:** Allow updates with audit log
**Why not chosen:** More complex, less secure

### Decision: Return Transaction ID in API Response
Both withdrawal and transfer responses include transaction IDs.

**Rationale:**
- Client can display receipt number
- Enables "view transaction details" feature
- Support for disputes/chargebacks
- Idempotency key for retries

### Decision: Separate Transaction per Account in Transfers
Create 2 transactions instead of 1 shared transaction.

**Rationale:**
- Each account has complete history
- Easy to query "all transactions for account X"
- Mirrors real banking systems
- Simplifies accounting

**Alternative considered:** Single transaction with direction flag
**Why not chosen:** Harder to query, less clear audit trail

### Decision: Store Balance After Transaction
`balanceAfter` field captures account balance immediately after transaction.

**Rationale:**
- Point-in-time snapshot
- Detect balance inconsistencies
- Reconstruct history without recalculating
- Audit verification

**Example verification:**
```
Transaction N-1: balanceAfter = 1000
Transaction N: amount = -100, fee = -1, balanceAfter = 899
Verify: 1000 - 100 - 1 = 899 ✓
```

### Decision: Card Reference in All Transactions
Even deposits from transfers include the originating card.

**Rationale:**
- Complete audit trail shows which card initiated transfer
- Receiver can see where money came from
- Fraud detection (pattern analysis)

## Compliance Considerations

### Data Retention
Transactions are kept indefinitely for:
- Legal compliance (7+ years typical)
- Dispute resolution
- Tax auditing
- Fraud investigation

### GDPR Right to Erasure
Transaction data is NOT subject to erasure (legal basis: contractual necessity).

**When user deletes account:**
- Personal details (name, email) can be anonymized
- Transaction records remain with pseudonymized ID
- Account number and amounts preserved

### Audit Log Access
Query pattern:
```java
@Query("""
    SELECT t FROM Transaction t
    WHERE t.account.user.id = :userId
    AND t.createdAt BETWEEN :startDate AND :endDate
    ORDER BY t.createdAt DESC
    """)
List<Transaction> findUserTransactions(
    @Param("userId") UUID userId,
    @Param("startDate") Instant startDate,
    @Param("endDate") Instant endDate
);
```

## Test Verification

### Integration Test Example
```java
@Test
void shouldCreateAuditTrailForWithdrawal() {
    // Act
    WithdrawalResponseDTO response = withdrawalService.withdraw(request);
    
    // Assert - transaction was created
    UUID transactionId = UUID.fromString(response.transactionId());
    Transaction transaction = transactionRepository.findById(transactionId)
        .orElseThrow();
    
    assertThat(transaction.getType()).isEqualTo(TransactionType.WITHDRAWAL);
    assertThat(transaction.getAmount()).isEqualByComparingTo("100.00");
    assertThat(transaction.getFee()).isEqualByComparingTo("1.00");
    assertThat(transaction.getBalanceAfter()).isEqualByComparingTo("899.00");
    assertThat(transaction.getCreatedAt()).isNotNull();
}

@Test
void shouldCreateLinkedTransactionsForTransfer() {
    // Act
    TransferResponseDTO response = transferService.transfer(request);
    
    // Assert - two linked transactions exist
    UUID transferId = UUID.fromString(response.transferTransactionId());
    UUID depositId = UUID.fromString(response.depositTransactionId());
    
    Transaction transferOut = transactionRepository.findById(transferId)
        .orElseThrow();
    Transaction transferIn = transactionRepository.findById(depositId)
        .orElseThrow();
    
    // Verify linkage
    assertThat(transferOut.getType()).isEqualTo(TransactionType.TRANSFER);
    assertThat(transferOut.getRelatedAccount()).isEqualTo(toAccount);
    
    assertThat(transferIn.getType()).isEqualTo(TransactionType.DEPOSIT);
    assertThat(transferIn.getRelatedTransaction()).isEqualTo(transferOut);
}
```

## Reporting Capabilities

With this audit structure, you can generate:
- Account statements
- Transaction receipts
- Fee summaries
- Transfer reports
- Balance reconciliation
- Fraud detection patterns
- Regulatory compliance reports
