# Requirement: A negative balance is not possible

## Implementation Strategy

### 1. Domain Layer Protection
The invariant "balance never negative" is enforced at the `Account` aggregate root:

```java
public void withdraw(BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Amount must be positive");
    }
    if (this.balance.compareTo(amount) < 0) {
        throw new InsufficientFundsException(this.id, this.balance, amount);
    }
    this.balance = this.balance.subtract(amount);
}
```

**Why in Domain?**
- The invariant is a core business rule
- All operations that modify balance must go through this method
- No `setBalance()` public method exists to bypass validation

### 2. Database Layer Defense
Additional constraint as defense in depth:

```sql
balance NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (balance >= 0)
```

**Why also in DB?**
- Last line of defense if application logic is bypassed
- Protects against direct SQL modifications
- Database-level data integrity

### 3. Service Layer Delegation
Services do NOT duplicate balance validation:

```java
// ✅ Correct - delegates to domain
account.withdraw(totalAmount);

// ❌ Wrong - duplicates validation
if (account.getBalance().compareTo(totalAmount) < 0) {
    throw new InsufficientFundsException(...);
}
account.setBalance(account.getBalance().subtract(totalAmount));
```

## Test Coverage

### Unit Tests (AccountTest.java)
- `shouldHaveZeroBalanceWhenCreated()` - Initial state
- `shouldLeaveZeroBalanceWhenWithdrawingExactAmount()` - Edge case
- `shouldThrowInsufficientFundsExceptionWhenWithdrawingMoreThanBalance()` - Validation
- `shouldProvideCorrectDataInInsufficientFundsException()` - Exception data
- `shouldNeverAllowNegativeBalanceAfterMultipleOperations()` - Complex scenario

### Integration Tests
- `WithdrawalServiceIntegrationTest.shouldRejectWithdrawalThatWouldCauseNegativeBalance()`
- `TransferServiceIntegrationTest.shouldFailWhenInsufficientFunds()`

## Design Decision Rationale

**Q: Why not validate in Service layer instead?**

A: Domain-driven design principle - the domain model protects its invariants. If validation were only in the service:
- Multiple services could duplicate validation logic
- Direct domain access could bypass validation
- The model becomes anemic (just data, no behavior)

**Q: Why throw custom `InsufficientFundsException` instead of generic exception?**

A: Provides rich context for clients:
- Available balance
- Requested amount
- Shortfall amount
- Account ID

This enables better error messages and client-side handling.
```
