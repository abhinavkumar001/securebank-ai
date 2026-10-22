package com.securebank.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByReference(String reference);

    @Query("""
            select t from Transaction t
            where t.fromAccountNumber = :accountNumber or t.toAccountNumber = :accountNumber
            order by t.createdAt desc, t.id desc
            """)
    Page<Transaction> findStatement(@Param("accountNumber") String accountNumber, Pageable pageable);

    // ----- features for the fraud model -----

    @Query("""
            select avg(t.amount) from Transaction t
            where t.fromAccountNumber = :accountNumber and t.type = :type and t.status = :status
            """)
    Double averageAmount(@Param("accountNumber") String accountNumber,
                         @Param("type") TransactionType type,
                         @Param("status") TransactionStatus status);

    long countByFromAccountNumberAndToAccountNumberAndStatus(String from, String to, TransactionStatus status);

    long countByFromAccountNumberAndTypeAndCreatedAtAfter(String from, TransactionType type, Instant after);

    // ----- insights -----

    /** Money that left the customer's accounts to someone else, grouped by category. */
    @Query("""
            select t.category, sum(t.amount) from Transaction t
            where t.status = :status
              and t.fromAccountNumber in :accounts
              and (t.toAccountNumber is null or t.toAccountNumber not in :accounts)
              and t.createdAt >= :from and t.createdAt < :to
            group by t.category
            """)
    List<Object[]> spendingByCategory(@Param("accounts") Collection<String> accounts,
                                      @Param("status") TransactionStatus status,
                                      @Param("from") Instant from, @Param("to") Instant to);

    /** Money that came into the customer's accounts from outside. */
    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.status = :status
              and t.toAccountNumber in :accounts
              and (t.fromAccountNumber is null or t.fromAccountNumber not in :accounts)
              and t.createdAt >= :from and t.createdAt < :to
            """)
    BigDecimal moneyIn(@Param("accounts") Collection<String> accounts,
                       @Param("status") TransactionStatus status,
                       @Param("from") Instant from, @Param("to") Instant to);

    List<Transaction> findByFromAccountNumberInAndStatus(Collection<String> accounts, TransactionStatus status);

    long countByCreatedAtAfter(Instant after);
}
