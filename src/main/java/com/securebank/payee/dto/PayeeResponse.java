package com.securebank.payee.dto;

import com.securebank.payee.Payee;

import java.time.Instant;

public record PayeeResponse(Long id, String name, String accountNumber, Instant createdAt) {

    public static PayeeResponse from(Payee p) {
        return new PayeeResponse(p.getId(), p.getName(), p.getAccountNumber(), p.getCreatedAt());
    }
}
