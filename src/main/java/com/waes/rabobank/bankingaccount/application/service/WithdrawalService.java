package com.waes.rabobank.bankingaccount.application.service;

import com.waes.rabobank.bankingaccount.application.dto.WithdrawalRequestDTO;
import com.waes.rabobank.bankingaccount.application.dto.WithdrawalResponseDTO;
import com.waes.rabobank.bankingaccount.domain.enums.CardStatus;
import com.waes.rabobank.bankingaccount.domain.enums.TransactionType;
import com.waes.rabobank.bankingaccount.domain.model.Account;
import com.waes.rabobank.bankingaccount.domain.model.Card;
import com.waes.rabobank.bankingaccount.domain.model.Transaction;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.AccountRepository;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.CardRepository;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.TransactionRepository;
import com.waes.rabobank.bankingaccount.shared.exception.*;
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

        // validate account
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
        // Validate card
        Card card = cardRepository.findById(cardId).orElseThrow(() -> new CardNotFoundException(cardId));

        // Workflow validations
        if (!card.getAccount().getId().equals(account.getId())) {
            throw new CardAccountMismatchException(cardId, accountId);
        }
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new InactiveCardException(cardId);
        }

        // Calculate fee (polymorphism in action)
        BigDecimal fee = card.calculateFee(request.amount());
        BigDecimal totalAmount = request.amount().add(fee);

        // Validate sufficient balance
        if (account.getBalance().compareTo(totalAmount) < 0) {
            throw new InsufficientFundsException(accountId, account.getBalance(), totalAmount);
        }

        // Deduct amount from account balance
        account.withdraw(totalAmount);
        accountRepository.save(account);

        // Record transaction
        var transaction = new Transaction(
                account,
                card,
                TransactionType.WITHDRAWAL,
                request.amount(),
                fee,
                account.getBalance()
        );
        transactionRepository.save(transaction);

        return new WithdrawalResponseDTO(
                // TODO: validate usage of transactionId as public part of the response
                account.getId().toString(),
                account.getCard().getId().toString(),
                request.amount(),
                fee,
                account.getBalance()
        );
    }
}
