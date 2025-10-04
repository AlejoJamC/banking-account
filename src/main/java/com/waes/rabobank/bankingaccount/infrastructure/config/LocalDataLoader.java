package com.waes.rabobank.bankingaccount.infrastructure.config;

import com.waes.rabobank.bankingaccount.domain.model.Account;
import com.waes.rabobank.bankingaccount.domain.model.DebitCard;
import com.waes.rabobank.bankingaccount.domain.model.User;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.AccountRepository;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.CardRepository;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * This class is used to load local data when the application is started with the "local" profile.
 */
@Component
@Profile("local")
public class LocalDataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(LocalDataLoader.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;

    public LocalDataLoader(UserRepository userRepository, AccountRepository accountRepository, CardRepository cardRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            logger.info("Seed data already exists. Skipping data loading...");
            return;
        }

        logger.info("Loading seed data for local development...");

        createSeedData();

        logger.info("Seed data loading completed.");
        logger.info("Users count: {}", userRepository.count());
        logger.info("Accounts count: {}", accountRepository.count());
        logger.info("Cards count: {}", cardRepository.count());
    }

    private void createSeedData() {
        // User 1: Alejandro Mantilla
        User alejandro = new User(
                "alejandro.mantilla@rabobank.nl",
                "Alejandro Mantilla",
                "123456789"
        );
        userRepository.save(alejandro);
        // Add accounts and cards for Alejandro
        // Create first account
        Account alejandroAccount1 = new Account(alejandro, "NL01RABO0123456789");
        accountRepository.save(alejandroAccount1);
        // Create debit card for first account
        DebitCard alejandroCard1 = new DebitCard(alejandroAccount1, "1234-5678-9012-3456", YearMonth.of(2027, 3));
        cardRepository.save(alejandroCard1);
        // Join card to account, bidirectional relationship
        alejandroAccount1.setCard(alejandroCard1);
    }
}
