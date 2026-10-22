package com.securebank.support;

public enum TicketCategory {
    FAILED_TRANSFER("Failed or held transfer"),
    FRAUD_REPORT("Fraud report"),
    CARD_ISSUE("Card issue"),
    ACCOUNT_ACCESS("Login or access"),
    PROFILE_UPDATE("Profile update"),
    GENERAL("General question");

    private final String label;

    TicketCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
