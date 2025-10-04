package com.waes.rabobank.bankingaccount.infrastructure.config;

import com.waes.rabobank.bankingaccount.domain.model.Account;
import com.waes.rabobank.bankingaccount.domain.model.CreditCard;
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
        DebitCard alejandroCard1 = new DebitCard(alejandroAccount1, "1234-5678-9012-1111", YearMonth.of(2027, 3));
        cardRepository.save(alejandroCard1);
        // Join card to account, bidirectional relationship
        alejandroAccount1.setCard(alejandroCard1);
        // Create second account, Credit Card
        Account alejandroAccount2 = new Account(alejandro, "NL01RABO1122334455");
        accountRepository.save(alejandroAccount2);
        CreditCard alejandroCard2 = new CreditCard(alejandroAccount2, "4321-8765-2109-1112", YearMonth.of(2029, 9));
        cardRepository.save(alejandroCard2);
        alejandroAccount2.setCard(alejandroCard2);

        // User 2: John Debit Doe
        User john = new User(
                "john@rabobank.nl",
                "John Debit Doe",
                "987654321"
        );
        userRepository.save(john);
        Account johnAccount1 = new Account(john, "NL02RABO9876543210");
        accountRepository.save(johnAccount1);
        DebitCard johnCard1 = new DebitCard(johnAccount1, "6543-0000-0000-2222", YearMonth.of(2026, 12));
        cardRepository.save(johnCard1);
        johnAccount1.setCard(johnCard1);

        // User 3: Jane Credit Smith
        User jane = new User(
                "jane@rabobank.nl",
                "Jane Credit Smith",
                "555666777"
        );
        userRepository.save(jane);
        Account janeAccount1 = new Account(jane, "NL03RABO5556667778");
        accountRepository.save(janeAccount1);
        CreditCard janeCard1 = new CreditCard(janeAccount1, "7777-0000-0000-3333", YearMonth.of(2028, 6));
        cardRepository.save(janeCard1);
        janeAccount1.setCard(janeCard1);
    }
}
