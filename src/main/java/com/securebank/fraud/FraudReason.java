package com.securebank.fraud;

/** A reason code (for reports) plus a sentence a reviewer can read. */
public record FraudReason(String code, String message) {}
