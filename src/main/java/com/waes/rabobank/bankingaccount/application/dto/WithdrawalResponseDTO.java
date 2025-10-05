package com.waes.rabobank.bankingaccount.application.dto;

import java.math.BigDecimal;

public record WithdrawalResponseDTO(
        // Pending add transactionId when implemented
        String accountId,
        String cardId,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal newBalance
) {
}
