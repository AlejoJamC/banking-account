package com.waes.rabobank.bankingaccount.application.dto;

import java.math.BigDecimal;

public record WithdrawalResponse(
        String accountId,
        BigDecimal newBalance,
        String cardId
) {
}
