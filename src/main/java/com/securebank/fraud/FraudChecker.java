package com.securebank.fraud;

import com.securebank.account.Account;

import java.math.BigDecimal;

/**
 * The transaction module depends only on this interface.
 * Step 2 ships a simple rule; Step 3 replaces it with the ML model + reason codes
 * without touching any transaction code.
 */
public interface FraudChecker {

    FraudDecision check(Account from, Account to, BigDecimal amount);
}
