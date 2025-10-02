package com.waes.rabobank.bankingaccount.infrastructure.rest;

import com.waes.rabobank.bankingaccount.application.dto.AccountResponse;
import com.waes.rabobank.bankingaccount.application.dto.WithdrawalRequest;
import com.waes.rabobank.bankingaccount.application.dto.WithdrawalResponse;
import com.waes.rabobank.bankingaccount.application.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final WithdrawalService withdrawalService;

    public AccountController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable UUID id) {
        // Dummy implementation
        return new AccountResponse(
                id.toString(),
                1000.0,
                "USD",
                "John Doe",
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
    public ResponseEntity<WithdrawalResponse> withdraw(
            @PathVariable String accountId,
            @Valid @RequestBody WithdrawalRequest request
    ) {
        // Matching accountId path and body request
        // Validate that accountId matches request.accountId, else throw exception
        // Create a custom exception handler to return 400 Bad Request
        if (!request.accountId().equals(accountId)) {
            throw new IllegalArgumentException("Account ID mismatch");
        }

        WithdrawalResponse response = withdrawalService.withdraw(request);
        return ResponseEntity.ok(response);
    }

}
