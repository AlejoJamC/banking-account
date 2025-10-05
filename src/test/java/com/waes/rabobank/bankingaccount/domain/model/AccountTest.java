package com.waes.rabobank.bankingaccount.domain.model;

import com.waes.rabobank.bankingaccount.domain.enums.AccountStatus;
import com.waes.rabobank.bankingaccount.shared.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private User testUser;
    private Account account;

    @BeforeEach
    void setUp() {
        testUser = new User("test@test.com", "Test User", "123456789");
        account = new Account(testUser, "NL01RABO0123456789");
    }

    // === Balance Tests ===
    @Test
    void shouldHaveZeroBalanceWhenCreated() {
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    void shouldIncreaseBalanceWhenDepositing() {
        account.deposit(BigDecimal.valueOf(100));

        assertEquals(BigDecimal.valueOf(100), account.getBalance());
    }

    @Test
    void shouldDecreaseBalanceWhenWithdrawing() {
        account.deposit(BigDecimal.valueOf(200));
        account.withdraw(BigDecimal.valueOf(50));

        assertEquals(BigDecimal.valueOf(150), account.getBalance());
    }

    @Test
    void shouldLeaveZeroBalanceWhenWithdrawingExactAmount() {
        account.deposit(BigDecimal.valueOf(100));
        account.withdraw(BigDecimal.valueOf(100));

        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    void shouldNeverAllowNegativeBalanceAfterMultipleOperations() {
        account.deposit(BigDecimal.valueOf(100));
        account.withdraw(BigDecimal.valueOf(30));

        assertThrows(InsufficientFundsException.class,
                () -> account.withdraw(BigDecimal.valueOf(80))
        );

        assertEquals(new BigDecimal("70"), account.getBalance());
    }

    // === Deposit Validations ===
    @Test
    void shouldFailWhenDepositingNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> account.deposit(BigDecimal.valueOf(-50))
        );
    }

    @Test
    void shouldFailWhenDepositingZero() {
        assertThrows(IllegalArgumentException.class,
                () -> account.deposit(BigDecimal.ZERO)
        );
    }

    // === Withdrawal Validations ===

    @Test
    void shouldFailWhenWithdrawingNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> account.withdraw(BigDecimal.valueOf(-10))
        );
    }

    @Test
    void shouldFailWhenWithdrawingZero() {
        assertThrows(IllegalArgumentException.class,
                () -> account.withdraw(BigDecimal.ZERO)
        );
    }

    @Test
    void shouldThrowInsufficientFundsExceptionWhenWithdrawingMoreThanBalance() {
        account.deposit(BigDecimal.valueOf(30));

        assertThrows(InsufficientFundsException.class,
                () -> account.withdraw(BigDecimal.valueOf(50))
        );
    }

    @Test
    void shouldProvideCorrectDataInInsufficientFundsException() {
        account.deposit(BigDecimal.valueOf(50));

        InsufficientFundsException exception = assertThrows(
                InsufficientFundsException.class,
                () -> account.withdraw(BigDecimal.valueOf(100))
        );

        assertEquals(new BigDecimal("50"), exception.getAvailableBalance());
        assertEquals(new BigDecimal("100"), exception.getRequestedAmount());
        assertEquals(new BigDecimal("50"), exception.getShortfall());
    }

    // === Card Association Tests ===

    @Test
    void shouldReturnFalseWhenNoCardIsAssociated() {
        assertFalse(account.hasCard());
    }

    @Test
    void shouldReturnTrueWhenCardIsAssociated() {
        DebitCard card = new DebitCard(account, "1234567890123456", YearMonth.of(2027, 12));
        account.setCard(card);

        assertTrue(account.hasCard());
    }

    // === Account Status Tests ===

    @Test
    void shouldBeActiveByDefault() {
        assertTrue(account.isActive());
    }

    @Test
    void shouldReturnTrueWhenStatusIsActive() {
        account.setStatus(AccountStatus.ACTIVE);

        assertTrue(account.isActive());
    }

    @Test
    void shouldReturnFalseWhenStatusIsSuspended() {
        account.setStatus(AccountStatus.SUSPENDED);

        assertFalse(account.isActive());
    }

    @Test
    void shouldReturnFalseWhenStatusIsClosed() {
        account.setStatus(AccountStatus.CLOSED);

        assertFalse(account.isActive());
    }
}
