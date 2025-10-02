package com.waes.rabobank.bankingaccount.domain.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

@Entity
@DiscriminatorValue("CREDIT")
public final class CreditCard extends Card {

    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.01"); // 1% fee, review location and a transform it to dynamic value

    protected CreditCard() {
    }

    public CreditCard(Account account, String cardNumber, YearMonth expiryDate) {
        super(account, cardNumber, expiryDate);
    }

    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(FEE_PERCENTAGE).setScale(4, RoundingMode.HALF_UP); // Review scale and rounding mode compare with banking market standards
    }
}
