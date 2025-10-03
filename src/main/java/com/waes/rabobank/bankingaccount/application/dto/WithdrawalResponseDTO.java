package com.waes.rabobank.bankingaccount.application.dto;

import java.math.BigDecimal;

public record WithdrawalResponseDTO(
        String accountId,
        BigDecimal newBalance,
        String cardId
) {
}
