package com.waes.rabobank.bankingaccount.infrastructure.rest;

import com.waes.rabobank.bankingaccount.application.dto.*;
import com.waes.rabobank.bankingaccount.application.service.AccountService;
import com.waes.rabobank.bankingaccount.application.service.TransferService;
import com.waes.rabobank.bankingaccount.application.service.WithdrawalService;
import com.waes.rabobank.bankingaccount.shared.exception.AccountIdMismatchException;
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
    private final TransferService transferService;

    public AccountController(AccountService accountService,
                             WithdrawalService withdrawalService,
                             TransferService transferService
    ) {
        this.accountService = accountService;
        this.withdrawalService = withdrawalService;
        this.transferService = transferService;
    }

    // Get All accounts balance of the authenticated user
    @GetMapping
    public List<AccountBalanceDTO> getAllAccounts(
            @RequestHeader("X-User-Id") String authenticatedUserId
    ) {
        UUID userId = UUID.fromString(authenticatedUserId);

        return accountService.getBalancesByUserId(userId);
    }

    // Withdraw
    @PostMapping("/{accountId}/withdraw")
    public WithdrawalResponseDTO withdraw(
            //@RequestHeader("X-User-Id") String authenticatedUserId,
            @PathVariable String accountId,
            @Valid @RequestBody WithdrawalRequestDTO request
    ) {
        if (!accountId.equals(request.accountId())) {
            throw new AccountIdMismatchException(accountId, request.accountId());
        }

        return withdrawalService.withdraw(request);
    }

    // Transfer
    @PostMapping("/{accountId}/transfer")
    public TransferResponseDTO transfer(
            //@RequestHeader("X-User-Id") String authenticatedUserId,
            @PathVariable String accountId,
            @Valid @RequestBody TransferRequestDTO request
    ) {
        if (!accountId.equals(request.fromAccountId())) {
            throw new AccountIdMismatchException(accountId, request.fromAccountId());
        }

        return transferService.transfer(request);
    }

    // Utils
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
}
