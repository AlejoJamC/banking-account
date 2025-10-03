package com.waes.rabobank.bankingaccount.domain.model;

import com.waes.rabobank.bankingaccount.domain.enums.AccountStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private final User dummyUser = new User(); // Replace with proper User mock if needed

    @Test
    void deposit_shouldIncreaseBalance() {
        Account account = new Account(dummyUser, "NL01RABO0123456789");
        account.deposit(BigDecimal.valueOf(100));
        assertEquals(BigDecimal.valueOf(100), account.getBalance());
    }

    @Test
    void deposit_shouldThrowExceptionForNegativeAmount() {
        Account account = new Account(dummyUser, "NL01RABO0123456789");
        assertThrows(IllegalArgumentException.class, () -> account.deposit(BigDecimal.valueOf(-50)));
    }

    @Test
    void withdraw_shouldDecreaseBalance() {
        Account account = new Account(dummyUser, "NL01RABO0123456789");
        account.deposit(BigDecimal.valueOf(200));
        account.withdraw(BigDecimal.valueOf(50));
        assertEquals(BigDecimal.valueOf(150), account.getBalance());
    }

    @Test
    void withdraw_shouldThrowExceptionForInsufficientFunds() {
        Account account = new Account(dummyUser, "NL01RABO0123456789");
        account.deposit(BigDecimal.valueOf(30));
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(BigDecimal.valueOf(50)));
    }

    @Test
    void withdraw_shouldThrowExceptionForNegativeAmount() {
        Account account = new Account(dummyUser, "NL01RABO0123456789");
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(BigDecimal.valueOf(-10)));
    }

//    @Test
//    void hasCard_shouldReturnTrueIfCardIsSet() {
//        Account account = new Account(dummyUser, "NL01RABO0123456789");
//        Card card = new Card();
//        account.setCard(card);
//        assertTrue(account.hasCard());
//    }

    @Test
    void hasCard_shouldReturnFalseIfCardIsNull() {
        Account account = new Account(dummyUser, "NL01RABO0123456789");
        assertFalse(account.hasCard());
    }

    @Test
    void isActive_shouldReturnTrueIfStatusIsActive() {
        Account account = new Account(dummyUser, "NL01RABO0123456789");
        account.setStatus(AccountStatus.ACTIVE);
        assertTrue(account.isActive());
    }

    @Test
    void isActive_shouldReturnFalseIfStatusIsNotActive() {
        Account account = new Account(dummyUser, "NL01RABO0123456789");
        account.setStatus(AccountStatus.SUSPENDED);
        assertFalse(account.isActive());
    }

    @Test
    void isActive_shouldReturnFalseIfStatusIsClosed() {
        Account account = new Account(dummyUser, "NL01RABO0123456789");
        account.setStatus(AccountStatus.CLOSED);
        assertFalse(account.isActive());
    }
}
