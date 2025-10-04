package com.waes.rabobank.bankingaccount.integration.service;

import com.waes.rabobank.bankingaccount.application.dto.WithdrawalRequestDTO;
import com.waes.rabobank.bankingaccount.application.service.WithdrawalService;
import com.waes.rabobank.bankingaccount.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WithdrawalServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WithdrawalService withdrawalService;

    @Test // testing happy path
    void shouldWithdrawSuccessfully() {
        // Arrange
        WithdrawalRequestDTO request = new WithdrawalRequestDTO(
                testAccount.getId().toString(),
                new BigDecimal("100.00"),
                testDebitCard.getId().toString()
        );
        // Act
        var response = withdrawalService.withdraw(request);

        // Assert
        assertThat(response.newBalance()).isEqualByComparingTo(new BigDecimal("900.00"));

        // Verify account balance in the database
        var reloadedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(reloadedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    @Test
    void shouldFailWithInsufficientFunds() {
        // Arrange
        // This account should have 1000.00 balance from setup
        WithdrawalRequestDTO request = new WithdrawalRequestDTO(
                testAccount.getId().toString(),
                new BigDecimal("2000.00"),
                testDebitCard.getId().toString()
        );

        // Act & Assert
        try {
            withdrawalService.withdraw(request);
        } catch (RuntimeException ex) { // review this later with custom exception, avoid NPE
            assertThat(ex.getMessage()).isEqualTo("Insufficient balance");
        }
    }
}
