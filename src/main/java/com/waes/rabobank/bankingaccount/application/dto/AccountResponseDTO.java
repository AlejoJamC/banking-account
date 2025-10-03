package com.waes.rabobank.bankingaccount.application.dto;

import java.math.BigDecimal;

public record AccountResponseDTO(
        String accountId,
        String accountNumber,
        BigDecimal balance,
        String currency,
        String accountName,
        String accountType,
        String status
) {
}
