package com.waes.rabobank.bankingaccount.application.dto;

import java.math.BigDecimal;

public record TransferResponseDTO(
        String transferTransactionId, // Transfer Out ID
        String depositTransactionId, // Transfer In ID
        String fromAccountId,
        String toAccountId,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal fromAccountBalanceAfter,
        BigDecimal toAccountBalanceAfter
) {
}
