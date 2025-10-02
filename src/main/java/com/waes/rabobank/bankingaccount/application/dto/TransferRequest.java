package com.waes.rabobank.bankingaccount.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Source account ID is required")
        String fromAccountId,

        @NotBlank(message = "Destination account ID is required")
        String toAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Card ID is required")
        String cardId
) {
    // Java 21 record features inherited: compact constructor, equals, hashCode, toString, inmutable fields, etc.
}
