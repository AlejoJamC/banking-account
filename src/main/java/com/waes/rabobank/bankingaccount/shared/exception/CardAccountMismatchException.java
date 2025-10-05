package com.waes.rabobank.bankingaccount.shared.exception;

import java.util.UUID;

public class CardAccountMismatchException extends RuntimeException {
    private final UUID cardId;
    private final UUID accountId;

    public CardAccountMismatchException(UUID cardId, UUID accountId) {
        super(String.format("Card %s does not belong to account %s", cardId, accountId));
        this.cardId = cardId;
        this.accountId = accountId;
    }

    public UUID getCardId() {
        return cardId;
    }

    public UUID getAccountId() {
        return accountId;
    }
}