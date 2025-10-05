package com.waes.rabobank.bankingaccount.application.service;

import com.waes.rabobank.bankingaccount.application.dto.TransferRequestDTO;
import com.waes.rabobank.bankingaccount.application.dto.TransferResponseDTO;
import com.waes.rabobank.bankingaccount.domain.enums.AccountStatus;
import com.waes.rabobank.bankingaccount.domain.enums.CardStatus;
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
public class TransferService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    public TransferService(
            AccountRepository accountRepository,
            CardRepository cardRepository,
            TransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransferResponseDTO transfer(TransferRequestDTO request) {
        UUID fromAccountId = UUID.fromString(request.fromAccountId());
        UUID toAccountId = UUID.fromString(request.toAccountId());
        UUID cardId = UUID.fromString(request.cardId());

        // 1. Load entities
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException(fromAccountId));

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException(toAccountId));

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        // 2. Workflow validations
        validateTransfer(fromAccount, toAccount, card);

        // 3. Calculate fee
        BigDecimal fee = card.calculateFee(request.amount());
        BigDecimal totalAmount = request.amount().add(fee);

        // 4. Execute transfer (domain validates balance)
        fromAccount.withdraw(totalAmount);
        toAccount.deposit(request.amount());

        // 5. Save accounts
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // 6. Create linked transactions
        Transaction transferOut = Transaction.transfer(
                fromAccount,
                card,
                request.amount(),
                fee,
                fromAccount.getBalance(),
                toAccount
        );
        transactionRepository.save(transferOut);

        Transaction transferIn = Transaction.deposit(
                toAccount,
                request.amount(),
                toAccount.getBalance(),
                transferOut
        );
        transactionRepository.save(transferIn);

        // 7. Return response
        return new TransferResponseDTO(
                transferOut.getId().toString(),
                transferIn.getId().toString(),
                fromAccount.getId().toString(),
                toAccount.getId().toString(),
                request.amount(),
                fee,
                fromAccount.getBalance(),
                toAccount.getBalance()
        );
    }

    private void validateTransfer(Account fromAccount, Account toAccount, Card card) {
        // Card belongs to fromAccount
        if (!card.getAccount().getId().equals(fromAccount.getId())) {
            throw new CardAccountMismatchException(card.getId(), fromAccount.getId());
        }

        // Card is active
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new InactiveCardException(card.getId());
        }

        // Cannot transfer to same account
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new SelfTransferException(fromAccount.getId());
        }

        // Both accounts must be active
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InactiveAccountException(fromAccount.getId());
        }

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InactiveAccountException(toAccount.getId());
        }
    }
}