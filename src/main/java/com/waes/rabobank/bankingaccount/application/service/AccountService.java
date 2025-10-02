package com.waes.rabobank.bankingaccount.application.service;

import com.waes.rabobank.bankingaccount.domain.model.Account;
import com.waes.rabobank.bankingaccount.infrastructure.persistence.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account findById(UUID id) {
        return accountRepository.findById(id).orElseThrow(() -> new RuntimeException("Account not found")); // Add custom exception
    }

    public List<Account> findAllAccounts() {
        return accountRepository.findAll();
    }

}
