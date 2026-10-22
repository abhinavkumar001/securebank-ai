package com.securebank.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByUserIdOrderByCreatedAtAsc(Long userId);

    boolean existsByAccountNumber(String accountNumber);

    long countByStatus(AccountStatus status);

    @Query("select coalesce(sum(a.balance), 0) from Account a")
    BigDecimal totalBalance();

    /**
     * SELECT ... FOR UPDATE: locks the row until the transaction ends,
     * so two requests can never change the same balance at the same time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
