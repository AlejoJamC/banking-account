# Requirement: Account should contain at least some user details, card details and current balance

## Implementation Strategy

### Profile-Based Data Loading

#### Local Development (`@Profile("local")`)
`LocalDataLoader` implements `CommandLineRunner` to populate initial data:

```java
@Component
@Profile("local")
public class LocalDataLoader implements CommandLineRunner {
    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            logger.info("Seed data already exists, skipping...");
            return;
        }
        createSeedData();
    }
}
```

**Why CommandLineRunner?**
- Executes after Spring context fully initialized
- Runs after Flyway migrations complete
- Schema guaranteed to exist

#### Test Environment (`@Profile("test")`)
`BaseIntegrationTest` uses `@BeforeEach` for test data:

```java
@BeforeEach
void setupTestData() {
    testUser = new User("test@test.com", "Test User", "000000001");
    userRepository.save(testUser);
    // ... create accounts and cards
}
```

**Why @BeforeEach instead of static?**
- `@Transactional` rolls back after each test
- Fresh data per test method
- No test pollution

## Seed Data Structure

### Local Development
```
User: Alejandro Mantilla (alejandro.mantilla@rabobank.nl)
├── Account 1: NL01RABO0123456789 (€5,000)
│   └── DebitCard: 1234567890121111 (exp: 2027-03)
└── Account 2: NL01RABO1122334455 (€5,000)
    └── CreditCard: 4321876521091112 (exp: 2029-09)

User: John Debit Doe (john@rabobank.nl)
└── Account: NL02RABO9876543210 (€500)
    └── DebitCard: 6543000000002222 (exp: 2026-12)

User: Jane Credit Smith (jane@rabobank.nl)
└── Account: NL03RABO5556667778 (€2,500)
    └── CreditCard: 7777000000003333 (exp: 2028-06)
```

## Design Decisions

### Decision: Hierarchy Order
```
1. User (no dependencies)
2. Account (depends on User)
3. Card (depends on Account)
```

**Rationale:** Foreign key constraints require parent entities exist first.

### Decision: No Flyway SQL for Seed Data
**Alternatives considered:**
- Flyway migration: `V100__seed_data.sql`

**Why not chosen:**
- Doesn't validate business rules (e.g., balance >= 0)
- Harder to generate UUIDs consistently
- Profile-based execution requires extra Flyway configuration

**Chosen approach:** Java code with domain validation
- Reuses domain logic (e.g., `account.deposit()`)
- Type-safe
- Conditional execution via Spring profiles

### Decision: Card Numbers Without Hyphens
Store: `"1234567890121111"` (16 digits)
Display: `"1234-5678-9012-1111"` (formatted)

**Rationale:**
- Database stores raw data (16 chars)
- Formatting is presentation concern (frontend)
- Validation and integration easier without format characters
- Standard in payment processing systems
