package com.waes.rabobank.bankingaccount.shared.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    private final UUID accountId;
    private final BigDecimal availableBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(
            UUID accountId,
            BigDecimal availableBalance,
            BigDecimal requestedAmount
    ) {
        super(String.format(
                "Insufficient funds for account %s: available balance is %s, requested amount is %s, shortfall is %s",
                accountId,
                availableBalance,
                requestedAmount,
                requestedAmount.subtract(availableBalance)
        ));
        this.accountId = accountId;
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public BigDecimal getShortfall() {
        return requestedAmount.subtract(availableBalance);
    }
}
