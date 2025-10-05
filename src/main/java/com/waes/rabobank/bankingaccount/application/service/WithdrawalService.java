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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WithdrawalService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    public WithdrawalService(
            AccountRepository accountRepository,
            CardRepository cardRepository,
            TransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public WithdrawalResponseDTO withdraw(WithdrawalRequestDTO request) {
        // Fetch account by ID
        UUID accountId = UUID.fromString(request.accountId());
        UUID cardId = UUID.fromString(request.cardId());

        // Load entities
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        // Workflow validations
        if (!card.getAccount().getId().equals(account.getId())) {
            throw new CardAccountMismatchException(cardId, accountId);
        }
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new InactiveCardException(cardId);
        }

        // Calculate fee
        BigDecimal fee = card.calculateFee(request.amount());
        BigDecimal totalAmount = request.amount().add(fee);

        // Execute withdrawal (domain validates balance)
        account.withdraw(totalAmount);
        accountRepository.save(account);

        // Create transaction for audit
        Transaction transaction = new Transaction(
                account,
                card,
                TransactionType.WITHDRAWAL,
                request.amount(),
                fee,
                account.getBalance()
        );
        transactionRepository.save(transaction);

        return new WithdrawalResponseDTO(
                transaction.getId().toString(),
                account.getId().toString(),
                card.getId().toString(),
                request.amount(),
                fee,
                account.getBalance()
        );
    }
}
