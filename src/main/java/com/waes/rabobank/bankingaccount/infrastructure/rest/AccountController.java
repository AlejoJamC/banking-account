package com.waes.rabobank.bankingaccount.infrastructure.rest;

import com.waes.rabobank.bankingaccount.application.dto.AccountBalanceDTO;
import com.waes.rabobank.bankingaccount.application.dto.AccountResponseDTO;
import com.waes.rabobank.bankingaccount.application.dto.WithdrawalRequestDTO;
import com.waes.rabobank.bankingaccount.application.dto.WithdrawalResponseDTO;
import com.waes.rabobank.bankingaccount.application.service.AccountService;
import com.waes.rabobank.bankingaccount.application.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final WithdrawalService withdrawalService;

    public AccountController(AccountService accountService, WithdrawalService withdrawalService) {
        this.accountService = accountService;
        this.withdrawalService = withdrawalService;
    }

    // Get All accounts balance of the current user with pagination
    @GetMapping
    public List<AccountBalanceDTO> getAllAccounts(
            @RequestHeader("X-User-Id") String authenticatedUserId
    ) {
        UUID userId = UUID.fromString(authenticatedUserId);

        return accountService.getBalancesByUserId(userId);
    }

    @GetMapping("/{id}")
    public AccountResponseDTO getAccount(@PathVariable UUID id) {
        // Dummy implementation
        return new AccountResponseDTO(
                id.toString(),
                "NL01RABO0123456789",
                BigDecimal.valueOf(1000.50),
                "EUR",
                "Main Account",
                "SAVINGS",
                "ACTIVE"
        );
    }

    @GetMapping("/count")
    public Long getAccountsCount() {
        // Dummy implementation
        return 600489147L;
    }

    // validate location and architecture
    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<WithdrawalResponseDTO> withdraw(
            @PathVariable String accountId,
            @Valid @RequestBody WithdrawalRequestDTO request
    ) {
        // Matching accountId path and body request
        // Validate that accountId matches request.accountId, else throw exception
        // Create a custom exception handler to return 400 Bad Request
        if (!request.accountId().equals(accountId)) {
            throw new IllegalArgumentException("Account ID mismatch");
        }

        WithdrawalResponseDTO response = withdrawalService.withdraw(request);
        return ResponseEntity.ok(response);
    }

}
