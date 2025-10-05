package com.waes.rabobank.bankingaccount.shared.exception;

import java.util.UUID;

public class CardNotFoundException extends RuntimeException {
    private final UUID cardId;

    public CardNotFoundException(UUID cardId) {
        super(String.format("Card with ID %s not found", cardId));
        this.cardId = cardId;
    }

    public UUID getCardId() {
        return cardId;
    }
}
