package com.waes.rabobank.bankingaccount.infrastructure.persistence;

import com.waes.rabobank.bankingaccount.domain.enums.TransactionType;
import com.waes.rabobank.bankingaccount.domain.model.Transaction;
import com.waes.rabobank.bankingaccount.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("""
            SELECT t 
            FROM Transaction t
            WHERE t.account.id = :accountId
            AND t.type = :type
            AND t.createdAt > :since
            ORDER BY t.createdAt DESC
            """)
    List<Transaction> findByAccountAndTypeSince(
            @Param("accountId") UUID accountId,
            @Param("type") TransactionType type,
            @Param("since") Instant since);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) 
            FROM Transaction t
            WHERE t.account.id = :accountId
            AND t.type = :type
            AND t.createdAt > :since
            """)
    BigDecimal sumAmountByAccountAndTypeSince(
            @Param("accountId") UUID accountId,
            @Param("type") TransactionType type,
            @Param("since") Instant since);
}
