package com.securebank.account.dto;

import com.securebank.account.Account;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(String accountNumber, String type, BigDecimal balance,
                              String status, Long userId, Instant createdAt) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getAccountNumber(), a.getType().name(), a.getBalance(),
                a.getStatus().name(), a.getUserId(), a.getCreatedAt());
    }
}
