package com.waes.rabobank.bankingaccount.shared.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    private final UUID userId;
    private final String email;

    public UserNotFoundException(UUID userId) {
        super(String.format("User with ID %s not found", userId));
        this.userId = userId;
        this.email = null;
    }
    public UserNotFoundException(String email) {
        super("User not found with email: " + email);
        this.userId = null;
        this.email = email;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}
