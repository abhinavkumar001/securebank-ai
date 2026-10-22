package com.securebank.fraud;

import com.securebank.account.Account;
import com.securebank.transaction.TransactionRepository;
import com.securebank.transaction.TransactionStatus;
import com.securebank.transaction.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;

/** Builds the model's features for one transfer from the account's history. */
@Component
public class FeatureExtractor {

    private final TransactionRepository transactions;
    private final Clock clock;

    public FeatureExtractor(TransactionRepository transactions, Clock clock) {
        this.transactions = transactions;
        this.clock = clock;
    }

    public FraudFeatures extract(Account from, Account to, BigDecimal amount) {
        Double average = transactions.averageAmount(from.getAccountNumber(), TransactionType.TRANSFER,
                TransactionStatus.SUCCESS);
        double ratio = average == null || average <= 0 ? 1.0 : amount.doubleValue() / average;

        boolean newRecipient = transactions.countByFromAccountNumberAndToAccountNumberAndStatus(
                from.getAccountNumber(), to.getAccountNumber(), TransactionStatus.SUCCESS) == 0;

        boolean oddHour = LocalTime.now(clock).getHour() < 5;

        int recent = (int) transactions.countByFromAccountNumberAndTypeAndCreatedAtAfter(
                from.getAccountNumber(), TransactionType.TRANSFER, clock.instant().minus(Duration.ofMinutes(10)));

        double share = from.getBalance().signum() == 0 ? 1.0
                : amount.divide(from.getBalance(), 4, RoundingMode.HALF_UP).doubleValue();

        return new FraudFeatures(ratio, newRecipient, oddHour, recent, share);
    }
}
