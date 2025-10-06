# Requirement: One rest endpoint to transfer money

## Endpoint Specification

```http
POST /api/accounts/{accountId}/transfer
Content-Type: application/json
```

### Request Body
```json
{
  "fromAccountId": "123abc...",
  "toAccountId": "456def...",
  "amount": 100.00,
  "cardId": "789ghi..."
}
```

### Response (200 OK)
```json
{
  "transferTransactionId": "tx-out-1",
  "depositTransactionId": "tx-in-2",
  "fromAccountId": "123abc...",
  "toAccountId": "456def...",
  "amount": 100.00,
  "fee": 1.00,
  "fromAccountBalanceAfter": 799.00,
  "toAccountBalanceAfter": 600.00
}
```

## Validation Rules

### Request Validations (400 Bad Request)
- `fromAccountId`: Not blank
- `toAccountId`: Not blank
- `amount`: Not null, minimum 0.01
- `cardId`: Not blank
- Path `{accountId}` must match body `fromAccountId`

### Business Validations (422 Unprocessable Entity)

#### SelfTransferException
```json
{
  "type": "https://api.rabobank.com/errors/self-transfer",
  "title": "Self Transfer Not Allowed",
  "status": 422,
  "detail": "Cannot transfer to the same account: 123abc...",
  "accountId": "123abc..."
}
```

#### InactiveAccountException
Either from or to account is SUSPENDED or CLOSED

#### InsufficientFundsException
Balance in from account < (amount + fee)

#### CardAccountMismatchException
Card doesn't belong to from account

#### InactiveCardException
Card status is BLOCKED or EXPIRED

### Entity Not Found (404)
- `AccountNotFoundException` - from or to account
- `CardNotFoundException`

## Transaction Creation

### Two Linked Transactions
```java
// 1. TRANSFER (outgoing)
Transaction transferOut = Transaction.transfer(
    fromAccount,
    card,
    amount,
    fee,
    fromAccount.getBalance(),
    toAccount  // relatedAccount
);

// 2. DEPOSIT (incoming)
Transaction transferIn = Transaction.deposit(
    toAccount,
    amount,
    toAccount.getBalance(),
    transferOut  // relatedTransaction
);
```

**Database relationships:**
```
transactions
├── transferOut (TRANSFER)
│   ├── account_id: 123 (from)
│   ├── related_account_id: 456 (to)
│   └── amount: 100, fee: 1
│
└── transferIn (DEPOSIT)
    ├── account_id: 456 (to)
    ├── related_transaction_id: transferOut.id
    └── amount: 100, fee: 0
```

**Why two transactions?**
- Audit trail in both accounts
- Each account has complete transaction history
- Easy to query "all transfers to/from account X"

## Service Layer Logic

```java
@Transactional
public TransferResponseDTO transfer(TransferRequestDTO request) {
    // 1. Load all entities
    Account fromAccount = accountRepository.findById(fromAccountId)...
    Account toAccount = accountRepository.findById(toAccountId)...
    Card card = cardRepository.findById(cardId)...

    // 2. Validate transfer constraints
    validateTransfer(fromAccount, toAccount, card);

    // 3. Calculate fee
    BigDecimal fee = card.calculateFee(request.amount());
    BigDecimal totalAmount = request.amount().add(fee);

    // 4. Execute transfer (domain methods)
    fromAccount.withdraw(totalAmount);  // Validates balance
    toAccount.deposit(request.amount()); // No fee on deposit

    // 5. Persist accounts
    accountRepository.save(fromAccount);
    accountRepository.save(toAccount);

    // 6. Create linked audit transactions
    Transaction transferOut = Transaction.transfer(...);
    transactionRepository.save(transferOut);

    Transaction transferIn = Transaction.deposit(..., transferOut);
    transactionRepository.save(transferIn);

    // 7. Return both transaction IDs
    return new TransferResponseDTO(...);
}
```

### Validation Method
```java
private void validateTransfer(Account from, Account to, Card card) {
    // Card belongs to from account
    if (!card.getAccount().getId().equals(from.getId())) {
        throw new CardAccountMismatchException(...);
    }

    // Card is active
    if (card.getStatus() != CardStatus.ACTIVE) {
        throw new InactiveCardException(...);
    }

    // No self-transfer
    if (from.getId().equals(to.getId())) {
        throw new SelfTransferException(...);
    }

    // Both accounts must be active
    if (from.getStatus() != AccountStatus.ACTIVE) {
        throw new InactiveAccountException(from.getId());
    }
    if (to.getStatus() != AccountStatus.ACTIVE) {
        throw new InactiveAccountException(to.getId());
    }
}
```

## Design Decisions

### Decision: Card Required for Transfer
Similar to withdrawal, transfers require card authentication.

**Rationale:**
- Security: proves ownership of from account
- Fee calculation: based on card type
- Audit: tracks which card initiated transfer

**Alternative considered:** Transfers without card
**Why not chosen:** Less secure, no fee differentiation

### Decision: Fee Only Charged to Sender
- From account: deducted `amount + fee`
- To account: receives `amount` only

**Rationale:**
- Standard banking practice
- Recipient shouldn't be penalized
- Sender bears cost of transaction

### Decision: Atomic Transaction with @Transactional
All operations in one database transaction:
- Withdraw from source
- Deposit to destination
- Create 2 transaction records

**Why critical?**
- If any step fails, entire operation rolls back
- No partial transfers (money doesn't disappear)
- Database consistency guaranteed

### Decision: Return Both Balances
Response includes:
- `fromAccountBalanceAfter`
- `toAccountBalanceAfter`

**Rationale:**
- Client sees immediate result
- No need for additional balance queries
- Useful for UI updates

### Decision: Self-Transfer Prevention
```java
if (from.getId().equals(to.getId())) {
    throw new SelfTransferException(...);
}
```

**Why prevent?**
- No business value (just moves money nowhere)
- Wastes transaction records
- Confusing in audit trail
- Fee would be charged for nothing

## Test Coverage

### Controller Tests (AccountControllerTest)
- `shouldTransferSuccessfully()` - Happy path
- `shouldReturn400WhenTransferAccountIdMismatch()` - Path/body validation
- `shouldReturn400WhenTransferRequestIsInvalid()` - DTO validation

### Integration Tests (TransferServiceIntegrationTest)
- `shouldTransferSuccessfullyWithDebitCard()` - 0% fee, both accounts updated
- `shouldTransferWithCreditCardAndApplyFee()` - 1% fee calculation
- `shouldFailWhenInsufficientFunds()` - Balance validation
- `shouldFailWhenFromAccountNotFound()` - 404 source
- `shouldFailWhenToAccountNotFound()` - 404 destination
- `shouldFailWhenCardNotFound()` - 404 card
- `shouldFailWhenCardDoesNotBelongToFromAccount()` - Ownership check
- `shouldFailWhenTransferringToSameAccount()` - Self-transfer prevention
- `shouldFailWhenCardIsInactive()` - Card status BLOCKED
- `shouldFailWhenCardIsExpired()` - Card status EXPIRED
- `shouldFailWhenFromAccountIsInactive()` - Source account SUSPENDED
- `shouldFailWhenToAccountIsInactive()` - Destination account SUSPENDED

**Total: 12 integration tests** covering all validation paths

## Audit Query Examples

### Find all transfers between two accounts
```sql
SELECT * FROM transactions 
WHERE (account_id = ? AND related_account_id = ?)
   OR (account_id = ? AND related_transaction_id IN 
       (SELECT id FROM transactions WHERE account_id = ?))
ORDER BY created_at DESC;
```

### Reconstruct transfer pair
```sql
-- Get transfer out
SELECT * FROM transactions WHERE id = 'tx-out-1';

-- Get related deposit
SELECT * FROM transactions WHERE related_transaction_id = 'tx-out-1';
