package com.waes.rabobank.bankingaccount.shared.exception;

public class AccountIdMismatchException extends RuntimeException {

    private final String pathAccountId;
    private final String bodyAccountId;

    public AccountIdMismatchException(String pathAccountId, String bodyAccountId) {
        super(String.format(
                "Path accountId (%s) must match body accountId (%s)",
                pathAccountId,
                bodyAccountId
        ));
        this.pathAccountId = pathAccountId;
        this.bodyAccountId = bodyAccountId;
    }

    public String getPathAccountId() {
        return pathAccountId;
    }

    public String getBodyAccountId() {
        return bodyAccountId;
    }
}