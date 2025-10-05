package com.waes.rabobank.bankingaccount.integration.service;

import com.waes.rabobank.bankingaccount.application.dto.TransferRequestDTO;
import com.waes.rabobank.bankingaccount.application.dto.TransferResponseDTO;
import com.waes.rabobank.bankingaccount.application.service.TransferService;
import com.waes.rabobank.bankingaccount.domain.enums.CardStatus;
import com.waes.rabobank.bankingaccount.domain.model.*;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.*;
import com.waes.rabobank.bankingaccount.shared.exception.*;
import com.waes.rabobank.bankingaccount.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.*;

class TransferServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account destinationAccount;

    @BeforeEach
    void setupDestinationAccount() {
        String uniqueAccountNumber = "NL00TEST" + System.nanoTime();
        destinationAccount = new Account(testUser, uniqueAccountNumber);
        destinationAccount.deposit(new BigDecimal("500.00"));
        accountRepository.save(destinationAccount);
    }

    @Test
    void shouldTransferSuccessfullyWithDebitCard() {
        // Arrange
        TransferRequestDTO request = new TransferRequestDTO(
                testAccount.getId().toString(),
                destinationAccount.getId().toString(),
                new BigDecimal("100.00"),
                testDebitCard.getId().toString()
        );

        // Act
        TransferResponseDTO response = transferService.transfer(request);

        // Assert
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        assertThat(response.fee()).isEqualByComparingTo("0.00"); // Debit = 0% fee
        assertThat(response.fromAccountBalanceAfter()).isEqualByComparingTo("900.00");
        assertThat(response.toAccountBalanceAfter()).isEqualByComparingTo("600.00");

        // Verify accounts in DB
        Account reloadedFrom = accountRepository.findById(testAccount.getId()).orElseThrow();
        Account reloadedTo = accountRepository.findById(destinationAccount.getId()).orElseThrow();

        assertThat(reloadedFrom.getBalance()).isEqualByComparingTo("900.00");
        assertThat(reloadedTo.getBalance()).isEqualByComparingTo("600.00");

        // Verify 2 transactions created
        var transactions = transactionRepository.findByAccountId(testAccount.getId());
        assertThat(transactions).hasSize(1); // TRANSFER out

        var destTransactions = transactionRepository.findByAccountId(destinationAccount.getId());
        assertThat(destTransactions).hasSize(1); // DEPOSIT in
    }

    @Test
    void shouldTransferWithCreditCardAndApplyFee() {
        // Arrange - Create credit card
        CreditCard creditCard = new CreditCard(testAccount, "5000000000000001", YearMonth.of(2030, 12));
        cardRepository.save(creditCard);
        testAccount.setCard(creditCard);

        TransferRequestDTO request = new TransferRequestDTO(
                testAccount.getId().toString(),
                destinationAccount.getId().toString(),
                new BigDecimal("100.00"),
                creditCard.getId().toString()
        );

        // Act
        TransferResponseDTO response = transferService.transfer(request);

        // Assert
        assertThat(response.fee()).isEqualByComparingTo("1.00"); // Credit = 1% fee
        assertThat(response.fromAccountBalanceAfter()).isEqualByComparingTo("899.00"); // 1000 - 100 - 1
        assertThat(response.toAccountBalanceAfter()).isEqualByComparingTo("600.00"); // 500 + 100
    }

    @Test
    void shouldFailWhenInsufficientFunds() {
        // Arrange
        TransferRequestDTO request = new TransferRequestDTO(
                testAccount.getId().toString(),
                destinationAccount.getId().toString(),
                new BigDecimal("2000.00"),
                testDebitCard.getId().toString()
        );

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InsufficientFundsException.class)
                .satisfies(ex -> {
                    InsufficientFundsException ife = (InsufficientFundsException) ex;
                    assertThat(ife.getAvailableBalance()).isEqualByComparingTo("1000.00");
                    assertThat(ife.getRequestedAmount()).isEqualByComparingTo("2000.00");
                });
    }

    @Test
    void shouldFailWhenFromAccountNotFound() {
        // Arrange
        TransferRequestDTO request = new TransferRequestDTO(
                "99999999-9999-9999-9999-999999999999",
                destinationAccount.getId().toString(),
                new BigDecimal("100.00"),
                testDebitCard.getId().toString()
        );

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void shouldFailWhenToAccountNotFound() {
        // Arrange
        TransferRequestDTO request = new TransferRequestDTO(
                testAccount.getId().toString(),
                "99999999-9999-9999-9999-999999999999",
                new BigDecimal("100.00"),
                testDebitCard.getId().toString()
        );

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void shouldFailWhenCardNotFound() {
        // Arrange
        TransferRequestDTO request = new TransferRequestDTO(
                testAccount.getId().toString(),
                destinationAccount.getId().toString(),
                new BigDecimal("100.00"),
                "99999999-9999-9999-9999-999999999999"
        );

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void shouldFailWhenCardDoesNotBelongToFromAccount() {
        // Arrange - Create card for destination account
        DebitCard otherCard = new DebitCard(destinationAccount, "4000000000000003", YearMonth.of(2030, 12));
        cardRepository.save(otherCard);

        TransferRequestDTO request = new TransferRequestDTO(
                testAccount.getId().toString(),
                destinationAccount.getId().toString(),
                new BigDecimal("100.00"),
                otherCard.getId().toString()
        );

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(CardAccountMismatchException.class);
    }

    @Test
    void shouldFailWhenTransferringToSameAccount() {
        // Arrange
        TransferRequestDTO request = new TransferRequestDTO(
                testAccount.getId().toString(),
                testAccount.getId().toString(), // Same account
                new BigDecimal("100.00"),
                testDebitCard.getId().toString()
        );

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(SelfTransferException.class);
    }

    @Test
    void shouldFailWhenCardIsInactive() {
        // Arrange - Block card
        testDebitCard.setStatus(CardStatus.BLOCKED);
        cardRepository.save(testDebitCard);

        TransferRequestDTO request = new TransferRequestDTO(
                testAccount.getId().toString(),
                destinationAccount.getId().toString(),
                new BigDecimal("100.00"),
                testDebitCard.getId().toString()
        );

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InactiveCardException.class);
    }

    @Test
    void shouldFailWhenCardIsExpired() {
        // Arrange - Expire card
        testDebitCard.setStatus(CardStatus.EXPIRED);
        cardRepository.save(testDebitCard);

        TransferRequestDTO request = new TransferRequestDTO(
                testAccount.getId().toString(),
                destinationAccount.getId().toString(),
                new BigDecimal("100.00"),
                testDebitCard.getId().toString()
        );

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InactiveCardException.class);
    }

    @Test
    void shouldFailWhenFromAccountIsInactive() {
        // Arrange - Suspend from account
        testAccount.setStatus(com.waes.rabobank.bankingaccount.domain.enums.AccountStatus.SUSPENDED);
        accountRepository.save(testAccount);

        TransferRequestDTO request = new TransferRequestDTO(
                testAccount.getId().toString(),
                destinationAccount.getId().toString(),
                new BigDecimal("100.00"),
                testDebitCard.getId().toString()
        );

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InactiveAccountException.class);
    }

    @Test
    void shouldFailWhenToAccountIsInactive() {
        // Arrange - Suspend destination account
        destinationAccount.setStatus(com.waes.rabobank.bankingaccount.domain.enums.AccountStatus.SUSPENDED);
        accountRepository.save(destinationAccount);

        TransferRequestDTO request = new TransferRequestDTO(
                testAccount.getId().toString(),
                destinationAccount.getId().toString(),
                new BigDecimal("100.00"),
                testDebitCard.getId().toString()
        );

        // Act & Assert
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InactiveAccountException.class);
    }
}