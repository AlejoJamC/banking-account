package com.waes.rabobank.bankingaccount.application.dto;

public record AccountResponse(
        String accountNumber,
        Double balance,
        String currency,
        String accountName,
        String accountType,
        String status
) {
}
