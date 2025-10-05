package com.waes.rabobank.bankingaccount.shared.exception;

import java.util.UUID;

public class SelfTransferException extends RuntimeException {

    private final UUID accountId;

    public SelfTransferException(UUID accountId) {
        super(String.format("Cannot transfer to the same account: %s", accountId));
        this.accountId = accountId;
    }

    public UUID getAccountId() {
        return accountId;
    }
}
