package com.waes.rabobank.bankingaccount.infrastructure.persistence;

import com.waes.rabobank.bankingaccount.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    @Query("""
            SELECT a FROM Account a
            LEFT JOIN FETCH a.card
            WHERE a.id = :id
            """)
    Optional<Account> findByIdWithCard(@Param("id") UUID id);
}
