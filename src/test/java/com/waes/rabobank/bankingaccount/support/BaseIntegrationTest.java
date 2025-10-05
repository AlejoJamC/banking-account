package com.waes.rabobank.bankingaccount.support;

import com.waes.rabobank.bankingaccount.domain.model.*;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.AccountRepository;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.CardRepository;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Base class for integration tests sharing a JVM-wide singleton Postgres container.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    static {
        // Enable container reuse (effective only if testcontainers.reuse.enable=true is set
        // in src/test/resources/testcontainers.properties
        POSTGRES.withReuse(true);
        // Start once per JVM; keep it outside of JUnit's @Testcontainers lifecycle
        POSTGRES.start();
    }

    // === Seed support ===
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected AccountRepository accountRepository;
    @Autowired
    protected CardRepository cardRepository;

    protected User testUser;
    protected Account testAccount;
    protected DebitCard testDebitCard;
    protected Account testCreditCardAccount;
    protected CreditCard testCreditCard;

    @BeforeEach
    void setupTestData() {
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
        testCreditCardAccount = new Account(testUser, "NL00TEST0000000002");
        testCreditCardAccount.deposit(new BigDecimal("2000.00"));
        accountRepository.save(testCreditCardAccount);

        testCreditCard = new CreditCard(testCreditCardAccount, "4000000000000002", YearMonth.of(2030, 11));
        cardRepository.save(testCreditCard);
        testCreditCardAccount.setCard(testCreditCard);
    }
}
