package com.waes.rabobank.bankingaccount.shared.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    private final UUID userId;

    public UserNotFoundException(UUID userId) {
        super(String.format("User with ID %s not found", userId));
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
