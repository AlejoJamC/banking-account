package com.waes.rabobank.bankingaccount.application.dto;

import java.math.BigDecimal;

public record WithdrawalResponseDTO(
        String accountId,
        String cardId,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal newBalance
) {
}
