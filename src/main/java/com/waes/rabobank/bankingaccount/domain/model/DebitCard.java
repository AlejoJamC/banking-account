package com.waes.rabobank.bankingaccount.domain.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@DiscriminatorValue("DEBIT") // review discriminator in single table inheritance
public final class DebitCard extends Card {

    protected DebitCard() {}

    public DebitCard(Account account, String cardNumber, YearMonth expiryDate) {
        super(account, cardNumber, expiryDate);
    }

    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        return BigDecimal.ZERO; // No fees for debit cards
    }
}
