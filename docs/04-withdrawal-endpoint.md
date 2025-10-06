# Requirement: One rest endpoint to withdraw money

## Endpoint Specification

```http
POST /api/accounts/{accountId}/withdraw
Content-Type: application/json
```

### Request Body
```json
{
  "accountId": "039b0af4-0995-4c1b-99dd-f4b05d8b73b4",
  "amount": 100.00,
  "cardId": "b96c9fa0-42ab-4a6f-9ff3-3ca9880a7aa0"
}
```

### Response (200 OK)
```json
{
  "transactionId": "789abc...",
  "accountId": "039b0af4-0995-4c1b-99dd-f4b05d8b73b4",
  "cardId": "b96c9fa0-42ab-4a6f-9ff3-3ca9880a7aa0",
  "amount": 100.00,
  "fee": 1.00,
  "balanceAfter": 899.00
}
```

## Validation Rules

### Request Validations (400 Bad Request)
- `accountId`: Not blank
- `amount`: Not null, minimum 0.01
- `cardId`: Not blank
- Path `{accountId}` must match body `accountId`

### Business Validations (422 Unprocessable Entity)

#### InsufficientFundsException
```json
{
  "type": "https://api.rabobank.com/errors/insufficient-funds",
  "title": "Insufficient Funds",
  "status": 422,
  "detail": "Insufficient funds in account...",
  "accountId": "039b0af4...",
  "availableBalance": 50.00,
  "requestedAmount": 100.00,
  "shortfall": 50.00
}
```

#### CardAccountMismatchException
Card doesn't belong to the account

#### InactiveCardException
Card status is BLOCKED or EXPIRED

### Entity Not Found (404)
- `AccountNotFoundException`
- `CardNotFoundException`

## Fee Calculation

### Polymorphic Strategy Pattern
```java
public abstract class Card {
    public abstract BigDecimal calculateFee(BigDecimal amount);
}

public class DebitCard extends Card {
    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        return BigDecimal.ZERO;  // 0%
    }
}

public class CreditCard extends Card {
    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.01");
    
    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(FEE_PERCENTAGE)
                     .setScale(4, RoundingMode.HALF_UP);  // 1%
    }
}
```

**Why Polymorphism?**
- Open/Closed Principle - easy to add new card types
- No if/switch statements on card type
- Each card encapsulates its fee logic

## Service Layer Orchestration

```java
@Transactional
public WithdrawalResponseDTO withdraw(WithdrawalRequestDTO request) {
    // 1. Load entities
    Account account = accountRepository.findById(accountId)
        .orElseThrow(() -> new AccountNotFoundException(accountId));
    
    Card card = cardRepository.findById(cardId)
        .orElseThrow(() -> new CardNotFoundException(cardId));

    // 2. Workflow validations (inter-aggregate)
    validateCardBelongsToAccount(card, account);
    validateCardIsActive(card);

    // 3. Calculate fee (polymorphic)
    BigDecimal fee = card.calculateFee(request.amount());
    BigDecimal totalAmount = request.amount().add(fee);

    // 4. Execute withdrawal (domain validates balance)
    account.withdraw(totalAmount);
    accountRepository.save(account);

    // 5. Create audit transaction
    Transaction transaction = new Transaction(
        account, card, WITHDRAWAL, 
        request.amount(), fee, account.getBalance()
    );
    transactionRepository.save(transaction);

    // 6. Return response
    return new WithdrawalResponseDTO(...);
}
```

## Design Decisions

### Decision: Total Amount = Amount + Fee
Balance is reduced by `amount + fee`, not just `amount`.

**Example:**
- Balance: 1000
- Withdrawal amount: 100
- Fee (credit card 1%): 1
- Total deducted: 101
- Balance after: 899

**Rationale:** Fee is a cost of the operation, not a separate charge.

### Decision: Card Required for Withdrawal
Even though it's a backend API, withdrawals require a card ID.

**Rationale:**
- Real-world withdrawals happen via card (ATM, POS)
- Enables fee calculation based on card type
- Audit trail shows which card was used

### Decision: Validation Order
1. Entity existence (404 fast-fail)
2. Workflow rules (422 business errors)
3. Balance validation (delegated to domain)

**Rationale:**
- Fail fast on missing data
- Separate technical errors (404) from business errors (422)
- Domain protects its invariants last

### Decision: Account ID in Both Path and Body
```
POST /api/accounts/{accountId}/withdraw
Body: { "accountId": "..." }
```

**Why redundant?**
- Path provides resource context (RESTful)
- Body provides operation data
- Validation ensures consistency (prevent mistakes)

**Alternative considered:** Only in path
**Why not chosen:** Request body would be incomplete/unclear

## Test Coverage

### Controller Tests (AccountControllerTest)
- `shouldWithdrawSuccessfully()` - Happy path
- `shouldReturn422WhenInsufficientFunds()` - Business rule
- `shouldReturn400WhenWithdrawalRequestIsInvalid()` - Validation
- `shouldReturn400WhenWithdrawalAccountIdMismatch()` - Consistency check

### Integration Tests (WithdrawalServiceIntegrationTest)
- `shouldWithdrawSuccessfully()` - Debit card (0% fee)
- `shouldWithdrawWithCreditCardAndApplyFee()` - Credit card (1% fee)
- `shouldRejectWithdrawalThatWouldCauseNegativeBalance()` - Balance protection
- `shouldFailWhenInsufficientFunds()` - Exception with context
- `shouldFailWhenCardNotFound()` - 404 handling
- `shouldFailWhenCardDoesNotBelongToAccount()` - 422 handling
- `shouldFailWhenCardIsInactive()` - Card status validation

### Domain Tests (AccountTest)
- 17 unit tests covering withdraw logic and invariants
