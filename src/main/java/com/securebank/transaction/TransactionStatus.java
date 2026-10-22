package com.securebank.transaction;

public enum TransactionStatus {
    SUCCESS,
    /** Stopped by the fraud check; money has NOT moved. Waits for admin review (Step 3). */
    HELD,
    REJECTED
}
