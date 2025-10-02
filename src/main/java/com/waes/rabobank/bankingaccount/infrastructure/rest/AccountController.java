package com.waes.rabobank.bankingaccount.infrastructure.rest;

import com.waes.rabobank.bankingaccount.application.dto.AccountResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

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
}
