package com.waes.rabobank.bankingaccount.shared.exception;

import java.util.UUID;

public class InactiveAccountException extends RuntimeException {

    private final UUID accountId;

    public InactiveAccountException(UUID accountId) {
        super(String.format("Account is not active: %s", accountId));
        this.accountId = accountId;
    }

    public UUID getAccountId() {
        return accountId;
    }
}