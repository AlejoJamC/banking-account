package com.waes.rabobank.bankingaccount.infrastructure.persistence;

import com.waes.rabobank.bankingaccount.domain.model.Transaction;
import com.waes.rabobank.bankingaccount.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
