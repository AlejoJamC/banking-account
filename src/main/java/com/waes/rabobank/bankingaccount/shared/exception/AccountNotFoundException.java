package com.waes.rabobank.bankingaccount.shared.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    private final UUID accountId;

    public AccountNotFoundException(UUID accountId) {
        super(String.format("Account with ID %s not found", accountId));
        this.accountId = accountId;
    }

    public UUID getAccountId() {
        return accountId;
    }
}
