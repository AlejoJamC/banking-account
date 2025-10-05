package com.waes.rabobank.bankingaccount.integration.repository;

import com.waes.rabobank.bankingaccount.domain.model.Account;
import com.waes.rabobank.bankingaccount.domain.model.User;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.AccountRepository;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AccountRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void accountStartsWithZeroBalance() {
        User user = new User("test@test.com", "Test User", "000000002");
        userRepository.save(user);

        Account account = new Account(user, "NL00TEST0000000002");
        accountRepository.save(account);

        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void accountCanHaveInitialBalance() {
        User user = new User("test2@test.com", "Test User 2", "000000003");
        userRepository.save(user);

        Account account = new Account(user, "NL00TEST0000000003");
        account.deposit(new BigDecimal("1000.00"));
        accountRepository.save(account);

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }
}
