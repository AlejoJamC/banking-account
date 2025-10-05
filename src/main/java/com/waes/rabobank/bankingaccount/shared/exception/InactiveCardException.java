package com.waes.rabobank.bankingaccount.shared.exception;

import java.util.UUID;

public class InactiveCardException extends RuntimeException {
    private final UUID cardId;

    public InactiveCardException(UUID cardId) {
        super(String.format("Card is not active: %s" , cardId));
        this.cardId = cardId;
    }

    public UUID getCardId() {
        return cardId;
    }
}
