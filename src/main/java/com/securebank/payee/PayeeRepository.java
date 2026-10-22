package com.securebank.payee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayeeRepository extends JpaRepository<Payee, Long> {

    List<Payee> findByUserIdOrderByNameAsc(Long userId);

    boolean existsByUserIdAndAccountNumber(Long userId, String accountNumber);
}
