# Architectural Decisions & Design Rationale

## Project Structure

### Clean Architecture + DDD Tactical Patterns
```
src/main/java/com/waes/rabobank/bankingaccount/
├── domain/
│   ├── model/          # Entities (Account, Card, User, Transaction)
│   └── enums/          # Value objects (AccountStatus, CardStatus, TransactionType)
├── application/
│   ├── service/        # Use cases (WithdrawalService, TransferService)
│   └── dto/            # Data Transfer Objects
├── infrastructure/
│   ├── rest/           # Controllers (AccountController)
│   ├── persistence/    # Repositories (JPA interfaces)
│   └── config/         # Configuration (JpaAuditing, LocalDataLoader)
└── shared/
└── exception/      # Custom exceptions + GlobalExceptionHandler
```

**Rationale:**
- **Domain layer** contains business logic and invariants
- **Application layer** orchestrates use cases
- **Infrastructure layer** handles technical concerns (HTTP, DB)
- **Dependencies point inward** (infrastructure → application → domain)

**Alternative considered:** Traditional layered architecture (controller → service → repository)
**Why not chosen:** 
- Tight coupling between layers
- Domain logic leaks into services
- Harder to test in isolation

---

## Pattern Decisions

### 1. Rich Domain Model vs Anemic Model

**Chosen: Rich Domain Model**

```java
// ✅ Rich Model - behavior + data
public class Account {
    private BigDecimal balance;
    
    public void withdraw(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(...);
        }
        this.balance = this.balance.subtract(amount);
    }
}

// ❌ Anemic Model - only data
public class Account {
    private BigDecimal balance;
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
```

**Rationale:**
- Encapsulates business rules
- Protects invariants (balance >= 0)
- Self-documenting (methods express intent)
- Prevents invalid state

**Trade-off:** More complex than simple getters/setters, but worth it for domain integrity.

---

### 2. Strategy Pattern for Fee Calculation

**Implementation: Polymorphic Card hierarchy**

```java
public abstract class Card {
    public abstract BigDecimal calculateFee(BigDecimal amount);
}

public class DebitCard extends Card {
    public BigDecimal calculateFee(BigDecimal amount) {
        return BigDecimal.ZERO;  // 0%
    }
}

public class CreditCard extends Card {
    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(new BigDecimal("0.01"));  // 1%
    }
}
```

**Rationale:**
- **Open/Closed Principle:** Easy to add new card types (e.g., PremiumCard)
- No if/switch statements on card type
- Each card encapsulates its fee logic

**Alternative considered:** Service with switch/if on card type
```java
// ❌ Not chosen
if (card.getType() == CardType.DEBIT) {
    fee = BigDecimal.ZERO;
} else if (card.getType() == CardType.CREDIT) {
    fee = amount.multiply(new BigDecimal("0.01"));
}
```
**Why not:** Violates Open/Closed, harder to extend

---

### 3. Factory Pattern for Transaction Creation

**Implementation: Static factory methods**

```java
public class Transaction {
    // Private constructor
    private Transaction(...) { }
    
    // Public factories
    public static Transaction withdrawal(...) { }
    public static Transaction transfer(...) { }
    public static Transaction deposit(...) { }
}
```

**Rationale:**
- **Descriptive names:** `Transaction.transfer()` vs `new Transaction()`
- **Encapsulation:** Hide complex construction logic
- **Type safety:** Can't create invalid transaction types

**Alternative considered:** Builder pattern
**Why not:** Overkill for objects with few fields

---

### 4. Exception Translation Pattern

**Implementation: GlobalExceptionHandler with @RestControllerAdvice**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handle(InsufficientFundsException ex) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY, 
            ex.getMessage()
        );
    }
}
```

**Rationale:**
- **Separation of concerns:** Domain throws business exceptions, infrastructure maps to HTTP
- **Centralized:** All exception handling in one place
- **Consistent:** Same error format (RFC 7807 ProblemDetail) across all endpoints

**Alternative considered:** Try/catch in each controller method
**Why not:** Code duplication, inconsistent error responses

---

### 5. Repository Pattern

**Implementation: Spring Data JPA interfaces**

```java
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    @Query("""
        SELECT a FROM Account a
        WHERE a.user.id = :userId
        AND a.status = 'ACTIVE'
        """)
    List<Account> findBalancesByUserId(@Param("userId") UUID userId);
}
```

**Rationale:**
- **Abstraction:** Domain doesn't know about JPA/SQL
- **Testability:** Can mock repositories in unit tests
- **Query encapsulation:** JPQL hidden from services

**Alternative considered:** Direct entity manager usage
**Why not:** More boilerplate, less type-safe

---

## Technology Decisions

### 1. Testcontainers over H2

**Decision: Use PostgreSQL via Testcontainers for integration tests**

**Rationale:**
- **Dialect consistency:** PostgreSQL in both test and production
- **No surprises:** Tests use same DB features (e.g., `gen_random_uuid()`, partial indexes)
- **False negatives eliminated:** H2 incompatibilities won't fail tests

**Trade-off:** Slower test startup (~5-10 seconds for container), but more reliable

**Alternative considered:** H2 in-memory database
**Why not:**
- Different SQL dialect causes compatibility issues
- Tests passed on H2 but failed on PostgreSQL
- Wasted time debugging dialect differences

---

### 2. Profile-Based Configuration

**Decision: Use Spring @Profile for environment-specific behavior**

```java
@Component
@Profile("local")
public class LocalDataLoader implements CommandLineRunner { }

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest { }
```

**Rationale:**
- **Clean separation:** Local seed data doesn't run in tests
- **Explicit:** Clear which code runs in which environment
- **Standard:** Spring's built-in mechanism

---

### 3. JPA Auditing

**Decision: Use @CreatedDate/@LastModifiedDate for timestamps**

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Account {
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
```

**Rationale:**
- **Automatic:** No manual `setCreatedAt()` calls
- **Consistent:** Same pattern across all entities
- **Less error-prone:** Can't forget to set timestamps

**Requires:** `@EnableJpaAuditing` in configuration class

---

### 4. UUID Primary Keys

**Decision: Use UUID over auto-increment integers**

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

**Rationale:**
- **Distributed systems:** Can generate IDs in application, no DB round-trip
- **Security:** Not sequential (harder to enumerate resources)
- **Merge-friendly:** No ID collisions when merging databases

**Trade-off:** Slightly larger index size, but negligible for this scale

---

## Code Quality Decisions

### 1. No Setters for Balance

**Decision: No `setBalance()` method**

```java
// ❌ Not allowed
public void setBalance(BigDecimal balance) {
    this.balance = balance;
}

// ✅ Only through business methods
public void withdraw(BigDecimal amount) { }
public void deposit(BigDecimal amount) { }
```

**Rationale:**
- Forces all balance changes through validated methods
- Prevents bypassing invariant checks
- Makes code intent clear (withdraw vs arbitrary setBalance)

---

### 2. Immutable DTOs with Records

**Decision: Use Java records for DTOs**

```java
public record WithdrawalRequestDTO(
    @NotBlank String accountId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank String cardId
) {}
```

**Rationale:**
- **Immutability:** Can't accidentally modify request data
- **Concise:** No boilerplate getters/equals/hashCode
- **Java 21 feature:** Modern syntax

---

### 3. Validation in Layers

**Decision: Different validations at each layer**

```
Controller:  DTO validation (@Valid, @NotBlank, @DecimalMin)
Service:     Workflow validation (card belongs to account, account is active)
Domain:      Invariant protection (balance >= 0)
```

**Rationale:**
- **Fast-fail:** Reject invalid requests early (controller)
- **Business rules:** Cross-aggregate checks in service
- **Data integrity:** Core invariants in domain

**Example:**
```java
// Controller - syntax validation
@Valid @RequestBody WithdrawalRequestDTO request

// Service - workflow validation
if (!card.getAccount().equals(account)) {
    throw new CardAccountMismatchException();
}

// Domain - invariant protection
if (balance.compareTo(amount) < 0) {
    throw new InsufficientFundsException();
}
```

---

### 4. Test Naming Convention

**Decision: `should[Behavior]When[Condition]` pattern**

```java
@Test
void shouldThrowInsufficientFundsExceptionWhenWithdrawingMoreThanBalance() { }

@Test
void shouldTransferSuccessfullyWithDebitCard() { }
```

**Rationale:**
- **Readable:** Test name describes what it tests
- **Consistent:** Same pattern across all tests
- **Self-documenting:** No need to read test body to understand purpose

---

## Pragmatic Trade-offs

### 1. Authentication/Authorization Not Implemented

**Decision: Assume upstream authentication**

```java
@GetMapping
public List<AccountBalanceDTO> getAllAccounts(
    @RequestHeader("X-User-Id") String authenticatedUserId
) { }
```

**Rationale:**
- Out of scope for assessment
- Assumes API Gateway handles auth (common in microservices)
- Simplifies testing

**Production requirement:** Implement OAuth2/JWT validation

---

### 2. CVV Storage Eliminated

**Decision: Remove CVV field from database**

**Rationale:**
- PCI-DSS Requirement 3.2: "Do not store CVV after authorization"
- Compliance > convenience
- CVV only needed at transaction time, not storage

---

### 3. Single GlobalExceptionHandler

**Decision: One file for all exception handlers**

**Rationale:**
- Only ~150 lines for 8 exceptions
- All business exceptions related (banking domain)
- Easy to find and review

**When to split:** If file exceeds 500 lines or multiple bounded contexts exist

---

### 4. No Optimistic Locking Tests

**Decision: `@Version` field added but not explicitly tested**

```java
@Version
private Long version;
```

**Rationale:**
- Concurrent withdrawal protection exists
- Testing race conditions requires complex setup
- Acceptable for assessment scope

**Production requirement:** Add concurrency tests with multiple threads

---

## Documentation Philosophy

### Why These Docs Exist

**Purpose:**
- Code review preparation
- Knowledge transfer
- Design decision justification
- Future maintenance reference

**Each doc answers:**
1. **What:** Requirement being fulfilled
2. **How:** Implementation approach
3. **Why:** Rationale for decisions
4. **Alternatives:** What was considered and rejected
5. **Tests:** How it's verified

---

## Key Takeaways

### What Makes This Architecture Good

1. **Testable:** 44 tests covering domain, service, and controller layers
2. **Maintainable:** Clear separation of concerns, each class has single responsibility
3. **Extensible:** Easy to add new card types, transaction types, validation rules
4. **Compliant:** Follows banking standards (no CVV storage, audit trail, immutable transactions)
5. **Pragmatic:** No over-engineering, appropriate patterns for scale

### What Could Be Improved (Future Iterations)

1. **Event Sourcing:** For complete audit trail with state reconstruction
2. **CQRS:** Separate read/write models for complex queries
3. **Saga Pattern:** For multi-step distributed transactions
4. **API Versioning:** Support backward compatibility
5. **Rate Limiting:** Prevent abuse
6. **Idempotency Keys:** Prevent duplicate transactions
7. **Async Processing:** For non-critical operations
8. **Caching:** Redis for frequently accessed balances
9. **Monitoring:** Prometheus metrics, distributed tracing
10. **Security:** OAuth2, field-level encryption

---

## Assumptions Documented

1. **Authentication:** X-User-Id header provided by upstream API Gateway
2. **CVV:** Not stored per PCI-DSS compliance
3. **Fee structure:** Debit 0%, Credit 1%
4. **Transaction immutability:** No updates/deletes on transactions
5. **Card limit:** One card per account
6. **Currency:** Single currency (EUR) system
7. **Testcontainers:** PostgreSQL for integration tests
8. **Profile-based seeding:** @Profile("local") for development data
