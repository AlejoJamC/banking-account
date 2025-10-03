package com.waes.rabobank.bankingaccount.application.service;

import com.waes.rabobank.bankingaccount.application.dto.AccountBalanceDTO;
import com.waes.rabobank.bankingaccount.domain.model.Account;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<AccountBalanceDTO> getBalancesByUserId(UUID userId) {

        return accountRepository.findBalancesByUserId(userId)
                .stream()
                .map(
                        account -> new AccountBalanceDTO(
                                account.getUser().getId().toString(),
                                account.getId().toString(),
                                account.getAccountNumber(),
                                account.getBalance(),
                                account.getCurrency()
                        )
                )
                .toList();
    }

    // Admin use case only
    public Page<AccountBalanceDTO> getAllAccounts(Pageable pageable) {
        return accountRepository.findAllAccounts(pageable);
    }

    public Account findById(UUID id) {
        return accountRepository.findById(id).orElseThrow(() -> new RuntimeException("Account not found")); // Add custom exception
    }

    public List<Account> findAllAccounts() {
        return accountRepository.findAll();
    }
}
