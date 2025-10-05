package com.waes.rabobank.bankingaccount.application.dto;

import java.math.BigDecimal;

public record WithdrawalResponseDTO(
        String transactionId,
        String accountId,
        String cardId,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal balanceAfter
) {
}