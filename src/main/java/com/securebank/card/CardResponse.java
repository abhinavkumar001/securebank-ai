package com.securebank.card;

public record CardResponse(Long id, String accountNumber, String maskedNumber, String holderName,
                           String expiry, boolean frozen) {

    public static CardResponse from(Card c) {
        return new CardResponse(c.getId(), c.getAccountNumber(),
                c.getFirstFour() + " •••• •••• " + c.getLastFour(),
                c.getHolderName(), c.getExpiry(), c.isFrozen());
    }
}
