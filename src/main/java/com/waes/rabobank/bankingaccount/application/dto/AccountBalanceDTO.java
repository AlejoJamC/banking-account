package com.waes.rabobank.bankingaccount.application.dto;

import java.math.BigDecimal;

public record AccountBalanceDTO(
        String userId,
        String accountId,
        String accountNumber,
        BigDecimal balance,
        String currency
) {
}
