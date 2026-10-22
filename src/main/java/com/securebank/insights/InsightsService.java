package com.securebank.insights;

import com.securebank.account.Account;
import com.securebank.account.AccountRepository;
import com.securebank.common.CurrentUser;
import com.securebank.common.Rupees;
import com.securebank.transaction.Transaction;
import com.securebank.transaction.TransactionRepository;
import com.securebank.transaction.TransactionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;

/** Money in/out and spending by category for the customer's home screen. */
@Service
public class InsightsService {

    public record CategoryAmount(String category, BigDecimal amount) {}

    public record Overview(String month, BigDecimal moneyIn, BigDecimal moneyOut, BigDecimal heldAmount,
                           int heldCount, List<CategoryAmount> spending, BigDecimal previousMonthOut, String tip) {}

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public InsightsService(AccountRepository accountRepository, TransactionRepository transactionRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Overview overview(CurrentUser user) {
        List<String> accounts = accountRepository.findByUserIdOrderByCreatedAtAsc(user.id()).stream()
                .map(Account::getAccountNumber).toList();
        YearMonth thisMonth = YearMonth.now(clock);
        String monthName = thisMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        if (accounts.isEmpty()) {
            return new Overview(monthName, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0,
                    List.of(), BigDecimal.ZERO, null);
        }

        ZoneId zone = clock.getZone();
        Instant start = thisMonth.atDay(1).atStartOfDay(zone).toInstant();
        Instant end = thisMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        Instant prevStart = thisMonth.minusMonths(1).atDay(1).atStartOfDay(zone).toInstant();

        Map<String, BigDecimal> current = spending(accounts, start, end);
        Map<String, BigDecimal> previous = spending(accounts, prevStart, start);

        BigDecimal out = current.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevOut = previous.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal in = transactionRepository.moneyIn(accounts, TransactionStatus.SUCCESS, start, end);

        List<Transaction> held = transactionRepository.findByFromAccountNumberInAndStatus(accounts, TransactionStatus.HELD);
        BigDecimal heldAmount = held.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryAmount> categories = current.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new CategoryAmount(e.getKey(), e.getValue()))
                .toList();

        return new Overview(monthName, in, out, heldAmount, held.size(), categories, prevOut,
                tip(current, previous, monthName));
    }

    private Map<String, BigDecimal> spending(List<String> accounts, Instant from, Instant to) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (Object[] row : transactionRepository.spendingByCategory(accounts, TransactionStatus.SUCCESS, from, to)) {
            result.put((String) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    /** Finds the category that grew the most since last month and says so in plain words. */
    private String tip(Map<String, BigDecimal> current, Map<String, BigDecimal> previous, String month) {
        if (current.isEmpty()) {
            return "No spending recorded in " + month + " yet.";
        }
        String biggestCategory = null;
        BigDecimal biggestIncrease = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : current.entrySet()) {
            BigDecimal increase = e.getValue().subtract(previous.getOrDefault(e.getKey(), BigDecimal.ZERO));
            if (previous.containsKey(e.getKey()) && increase.compareTo(biggestIncrease) > 0) {
                biggestIncrease = increase;
                biggestCategory = e.getKey();
            }
        }
        if (biggestCategory != null) {
            return "You’ve spent " + Rupees.format(biggestIncrease) + " more on " + biggestCategory.toLowerCase(Locale.ROOT)
                    + " than last month.";
        }
        Map.Entry<String, BigDecimal> top = Collections.max(current.entrySet(), Map.Entry.comparingByValue());
        return top.getKey() + " is your biggest expense in " + month + " at " + Rupees.format(top.getValue()) + ".";
    }
}
