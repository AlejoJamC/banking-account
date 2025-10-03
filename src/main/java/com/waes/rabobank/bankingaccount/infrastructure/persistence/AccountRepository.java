package com.waes.rabobank.bankingaccount.infrastructure.persistence;

import com.waes.rabobank.bankingaccount.application.dto.AccountBalanceDTO;
import com.waes.rabobank.bankingaccount.domain.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Query("""
            SELECT a
            FROM Account a
            WHERE a.user.id = :userId
            AND a.status = 'ACTIVE'
            """)
    List<Account> findBalancesByUserId(@Param("userId") UUID userId);

    // Admin use case only
    @Query("""
            SELECT new com.waes.rabobank.bankingaccount.application.dto.AccountBalanceDTO(
                CAST(a.user.id AS string),
                CAST(a.id AS string),
                a.accountNumber,
                a.balance,
                a.currency)
            FROM Account a
            """)
    Page<AccountBalanceDTO> findAllAccounts(Pageable pageable);
}
