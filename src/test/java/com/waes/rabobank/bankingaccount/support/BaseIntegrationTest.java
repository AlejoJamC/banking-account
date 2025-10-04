package com.waes.rabobank.bankingaccount.support;

import com.waes.rabobank.bankingaccount.domain.model.*;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.AccountRepository;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.CardRepository;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.YearMonth;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected AccountRepository accountRepository;
    @Autowired
    protected CardRepository cardRepository;

    protected User testUser;
    protected Account testAccount;
    protected DebitCard testDebitCard;
    protected CreditCard testCreditCard;

    @BeforeEach
    void setupTestData() {
        // User
        testUser = new User("test@test.com", "Test User", "000000001");
        userRepository.save(testUser);

        // Debit Card Account
        testAccount = new Account(testUser, "NL00TEST0000000001");
        testAccount.deposit(new BigDecimal("1000.00"));
        accountRepository.save(testAccount);

        testDebitCard = new DebitCard(testAccount, "4000000000000001", YearMonth.of(2030, 12));
        cardRepository.save(testDebitCard);
        testAccount.setCard(testDebitCard);

        // Credit Card Account
        Account creditAccount = new Account(testUser, "NL00TEST0000000002");
        creditAccount.deposit(new BigDecimal("2000.00"));
        accountRepository.save(creditAccount);

        testCreditCard = new CreditCard(creditAccount, "4000000000000002", YearMonth.of(2030, 11));
        cardRepository.save(testCreditCard);
        creditAccount.setCard(testCreditCard);
    }
}
