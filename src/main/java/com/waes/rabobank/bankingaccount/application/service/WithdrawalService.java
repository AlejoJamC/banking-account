package com.waes.rabobank.bankingaccount.application.service;

import com.waes.rabobank.bankingaccount.application.dto.WithdrawalRequestDTO;
import com.waes.rabobank.bankingaccount.application.dto.WithdrawalResponseDTO;
import com.waes.rabobank.bankingaccount.domain.enums.CardStatus;
import com.waes.rabobank.bankingaccount.domain.enums.TransactionType;
import com.waes.rabobank.bankingaccount.domain.model.Transaction;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.AccountRepository;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.CardRepository;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WithdrawalService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    public WithdrawalService(AccountRepository accountRepository, CardRepository cardRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public WithdrawalResponseDTO withdraw(WithdrawalRequestDTO request) {
        // Fetch account by ID
        UUID accountId = UUID.fromString(request.accountId());
        UUID cardId = UUID.fromString(request.cardId());
        var account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));

        // Validate card
        var card = cardRepository.findById(cardId).orElseThrow(() -> new RuntimeException("Card not found"));
        if (!card.getAccount().getId().equals(account.getId())) {
            throw new RuntimeException("Card does not belong to the account");
        }
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new RuntimeException("Card is not active");
        }

        // Validate sufficient balance
        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Deduct amount from account balance
        account.setBalance(account.getBalance().subtract(request.amount()));
        accountRepository.save(account);

        // Record transaction
        var transaction = new Transaction(
                account,
                card,
                TransactionType.WITHDRAWAL,
                request.amount(),
                BigDecimal.ZERO, // No fee for simplicity
                account.getBalance()
        );
        transactionRepository.save(transaction);

        return new WithdrawalResponseDTO(account.getId().toString(),
                account.getBalance(),
                account.getCard().getId().toString());
    }
}
