# Requirement: One rest endpoint to see current available balance in all accounts

## Endpoint Specification

```http
GET /api/accounts
X-User-Id: {userId}
```

### Response (200 OK)
```json
[
  {
    "userId": "123...",
    "accountId": "acc-1",
    "accountNumber": "NL01RABO0123456789",
    "balance": 5000.00,
    "currency": "EUR"
  }
]
```

### Error Responses

#### User Not Found (404)
```json
{
  "type": "https://api.rabobank.com/errors/user-not-found",
  "title": "User Not Found",
  "status": 404,
  "detail": "User not found: 123...",
  "userId": "123..."
}
```

#### Empty List (200 OK)
User exists but has no accounts - returns `[]`

**Rationale:** Empty collection is not an error state.

## Implementation Layers

### Controller
```java
@GetMapping
public List<AccountBalanceDTO> getAllAccounts(
    @RequestHeader("X-User-Id") String authenticatedUserId
) {
    UUID userId = UUID.fromString(authenticatedUserId);
    return accountService.getBalancesByUserId(userId);
}
```

### Service
```java
public List<AccountBalanceDTO> getBalancesByUserId(UUID userId) {
    if (!userRepository.existsById(userId)) {
        throw new UserNotFoundException(userId);
    }
    
    return accountRepository.findBalancesByUserId(userId)
        .stream()
        .map(account -> new AccountBalanceDTO(...))
        .toList();
}
```

### Repository
```java
@Query("""
    SELECT a
    FROM Account a
    WHERE a.user.id = :userId
    AND a.status = 'ACTIVE'
    """)
List<Account> findBalancesByUserId(@Param("userId") UUID userId);
```

**Why filter by ACTIVE?** Closed/suspended accounts shouldn't appear in balance inquiry.

## Design Decisions

### Decision: X-User-Id Header for Authentication
**Assumption:** Upstream API Gateway handles authentication and injects authenticated user ID.

**Alternative considered:** Extract from JWT token

**Why not chosen:**
- Out of scope for this assessment
- Simplifies testing
- Common pattern in microservices behind API gateway

### Decision: Return Empty List vs 404
**User exists, no accounts:** Return `[]` (200 OK)
**User doesn't exist:** Throw `UserNotFoundException` (404)

**Rationale:**
- Empty collection is valid state (new user)
- Missing user is error state
- Follows REST conventions

### Decision: Only ACTIVE Accounts
Query filters `status = 'ACTIVE'`

**Rationale:**
- Suspended/closed accounts shouldn't show in balance
- Business rule: only active accounts are "available"
