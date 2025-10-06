# Requirement: One credit card or debit card is linked with one account

## Relationship Specification

### Domain Model
```java
@Entity
public class Account {
    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private Card card;
}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "card_type")
public abstract class Card {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
```

**Key aspects:**
- `@OneToOne` - one account has exactly one card
- `mappedBy = "account"` - Card owns the relationship
- `cascade = CascadeType.ALL` - Card lifecycle bound to Account
- `orphanRemoval = true` - Delete card when unlinked

### Database Schema
```sql
CREATE TABLE cards (
    id UUID PRIMARY KEY,
    card_type VARCHAR(31) NOT NULL,
    account_id UUID NOT NULL,
    card_number VARCHAR(16) NOT NULL UNIQUE,
    expiry_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    
    CONSTRAINT uc_cards_account UNIQUE (account_id),
    CONSTRAINT fk_cards_account FOREIGN KEY (account_id) 
        REFERENCES accounts(id)
);
```

**Enforcement:**
- `UNIQUE (account_id)` - Database guarantees 1:1 relationship
- `NOT NULL account_id` - Card must always have an account
- Foreign key cascade prevents orphaned cards

## Polymorphic Card Hierarchy

### Single Table Inheritance
```
Card (abstract)
├── DebitCard
└── CreditCard
```

**Database representation:**
```sql
-- All cards in one table
card_type | id | account_id | card_number | ...
----------|----|-----------
DEBIT     | 1  | acc-1     | 4532...
CREDIT    | 2  | acc-2     | 5425...
```

**Why Single Table?**
- Simple queries (no joins needed)
- Good performance
- Easy to add new card types
- Shared columns for all cards

**Alternative considered:** Table per class
**Why not chosen:** Over-engineering for 2 card types

## Card Types

### DebitCard
```java
@Entity
@DiscriminatorValue("DEBIT")
public final class DebitCard extends Card {
    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        return BigDecimal.ZERO;  // No fee
    }
}
```

**Characteristics:**
- No transaction fees
- Direct account access
- Standard for everyday transactions

### CreditCard
```java
@Entity
@DiscriminatorValue("CREDIT")
public final class CreditCard extends Card {
    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.01");
    
    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(FEE_PERCENTAGE)
                     .setScale(4, RoundingMode.HALF_UP);
    }
}
```

**Characteristics:**
- 1% fee on transactions
- Credit facility (assumption: tracked elsewhere)
- Premium card type

## Bidirectional Synchronization

### Setting Card on Account
```java
public void setCard(Card card) {
    this.card = card;
    if (card != null && card.getAccount() != this) {
        card.setAccount(this);  // Sync both sides
    }
}
```

**Why bidirectional?**
- Navigate from Account → Card
- Navigate from Card → Account
- JPA requires both sides synced

**Common mistake:**
```java
// ❌ Wrong - only sets one side
account.setCard(card);

// ✅ Correct - syncs both sides
account.setCard(card);
card.setAccount(account);  // Handled automatically by setCard()
```

## Seed Data Examples

### Local Development
```java
// Account with DebitCard
Account account1 = new Account(user, "NL01RABO0123456789");
accountRepository.save(account1);

DebitCard debitCard = new DebitCard(
    account1, 
    "1234567890121111", 
    YearMonth.of(2027, 3)
);
cardRepository.save(debitCard);
account1.setCard(debitCard);  // Establishes 1:1 relationship
```

### Test Data
```java
@BeforeEach
void setupTestData() {
    testAccount = new Account(testUser, "NL00TEST0000000001");
    accountRepository.save(testAccount);
    
    testDebitCard = new DebitCard(
        testAccount, 
        "4000000000000001", 
        YearMonth.of(2030, 12)
    );
    cardRepository.save(testDebitCard);
    testAccount.setCard(testDebitCard);
}
```

## Design Decisions

### Decision: Card Owns Relationship
`Card.account` is not nullable, but `Account.card` is nullable.

**Rationale:**
- Card cannot exist without Account (strong dependency)
- Account can exist briefly without Card (during creation)
- Prevents orphaned cards

### Decision: Single Card per Account
One account cannot have multiple cards.

**Alternative considered:** One-to-many (multiple cards per account)
**Why not chosen:** Requirement explicitly states "one card is linked"

**Future extension:** If needed, change to `@OneToMany List<Card> cards`

### Decision: Abstract Card with Polymorphism
Card is abstract base class, not concrete.

**Rationale:**
- Forces choice between Debit/Credit
- No "generic" cards in system
- Type-safe fee calculation

### Decision: Cascade ALL + Orphan Removal
```java
cascade = CascadeType.ALL, orphanRemoval = true
```

**Behavior:**
- Delete account → card deleted automatically
- Remove card from account → card deleted automatically
- Save account → card saved automatically

**Rationale:** Card has no independent lifecycle from Account

## Validation in Tests

### Unit Tests (AccountTest)
```java
@Test
void shouldReturnFalseWhenNoCardIsAssociated() {
    assertFalse(account.hasCard());
}

@Test
void shouldReturnTrueWhenCardIsAssociated() {
    DebitCard card = new DebitCard(account, "1234...", YearMonth.of(2027, 12));
    account.setCard(card);
    assertTrue(account.hasCard());
}
```

### Database Constraint Test
Attempting to create two cards for same account:
```java
@Test
void shouldFailWhenCreatingSecondCardForSameAccount() {
    DebitCard card1 = new DebitCard(account, "1111...", YearMonth.of(2027, 12));
    cardRepository.save(card1);
    
    DebitCard card2 = new DebitCard(account, "2222...", YearMonth.of(2028, 6));
    
    assertThatThrownBy(() -> cardRepository.save(card2))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uc_cards_account");
}
```

## Query Patterns

### Find Account by Card
```java
@Query("SELECT c.account FROM Card c WHERE c.id = :cardId")
Optional<Account> findAccountByCardId(@Param("cardId") UUID cardId);
```

### Check if Account has Card
```java
public boolean hasCard() {
    return card != null;
}
```

### Get Card Type
```java
if (account.getCard() instanceof DebitCard) {
    // Handle debit
} else if (account.getCard() instanceof CreditCard) {
    // Handle credit
}
```

**Better approach:** Use polymorphism
```java
BigDecimal fee = account.getCard().calculateFee(amount);
// No need to check type
```
