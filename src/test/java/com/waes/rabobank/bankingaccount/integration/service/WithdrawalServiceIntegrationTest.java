package com.waes.rabobank.bankingaccount.integration.service;

import com.waes.rabobank.bankingaccount.application.dto.WithdrawalRequestDTO;
import com.waes.rabobank.bankingaccount.application.dto.WithdrawalResponseDTO;
import com.waes.rabobank.bankingaccount.application.service.WithdrawalService;
import com.waes.rabobank.bankingaccount.domain.enums.TransactionType;
import com.waes.rabobank.bankingaccount.domain.model.Account;
import com.waes.rabobank.bankingaccount.domain.model.Transaction;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.TransactionRepository;
import com.waes.rabobank.bankingaccount.shared.exception.InsufficientFundsException;
import com.waes.rabobank.bankingaccount.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WithdrawalServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
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
        assertThat(response.balanceAfter()).isEqualByComparingTo(new BigDecimal("900.00"));

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
        assertThatThrownBy(() -> withdrawalService.withdraw(request))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void shouldWithdrawWithDebitCard() {
        // Arrange
        WithdrawalRequestDTO request = new WithdrawalRequestDTO(
                testAccount.getId().toString(),
                new BigDecimal("100.00"),
                testDebitCard.getId().toString()
        );

        // Act
        WithdrawalResponseDTO response = withdrawalService.withdraw(request);

        // Assert
        assertThat(response.balanceAfter()).isEqualByComparingTo("900.00");
        assertThat(response.fee()).isEqualByComparingTo("0.00");

        // Verify Transaction created
        List<Transaction> transactions = transactionRepository.findByAccountId(testAccount.getId());
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getType()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    @Test
    void shouldWithdrawWithCreditCardAndApplyFee() {
        // Arrange
        WithdrawalRequestDTO request = new WithdrawalRequestDTO(
                testCreditCard.getAccount().getId().toString(), // Use credit account
                new BigDecimal("100.00"),
                testCreditCard.getId().toString() // Use credit card
        );

        // Act
        WithdrawalResponseDTO response = withdrawalService.withdraw(request);

        // Assert
        assertThat(response.fee()).isEqualByComparingTo("1.00");  // 1% fee
        assertThat(response.balanceAfter()).isEqualByComparingTo("1899.00");  // 2000 - 100 - 1
    }

    @Test
    void shouldRejectWithdrawalThatWouldCauseNegativeBalance() {
        // Arrange
        WithdrawalRequestDTO request = new WithdrawalRequestDTO(
                testAccount.getId().toString(),
                new BigDecimal("2000.00"),  // Exceeds balance
                testDebitCard.getId().toString()
        );

        // Act & Assert
        assertThatThrownBy(() -> withdrawalService.withdraw(request))
                .isInstanceOf(InsufficientFundsException.class);

        // Verify balance unchanged
        Account reloadedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(reloadedAccount.getBalance()).isEqualByComparingTo("1000.00");
    }
}
