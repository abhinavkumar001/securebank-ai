package com.securebank.admin;

import com.securebank.account.AccountRepository;
import com.securebank.account.AccountStatus;
import com.securebank.fraud.FraudAlert;
import com.securebank.fraud.FraudAlertRepository;
import com.securebank.fraud.FraudAlertStatus;
import com.securebank.support.TicketRepository;
import com.securebank.support.TicketStatus;
import com.securebank.support.Urgency;
import com.securebank.transaction.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminStatsService {

    public record Stats(BigDecimal totalDeposits, long accounts, long frozenAccounts, long transactionsToday,
                        long pendingReviews, BigDecimal pendingAmount, long openTickets, long urgentTickets,
                        Map<String, Long> flaggedReasons30d, long approved30d, long rejected30d) {}

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final FraudAlertRepository alerts;
    private final TicketRepository tickets;
    private final Clock clock;

    public AdminStatsService(AccountRepository accounts, TransactionRepository transactions,
                             FraudAlertRepository alerts, TicketRepository tickets, Clock clock) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.alerts = alerts;
        this.tickets = tickets;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Stats stats() {
        List<FraudAlert> pending = alerts.findByStatusOrderByRiskScoreDescCreatedAtAsc(FraudAlertStatus.PENDING);
        BigDecimal pendingAmount = pending.stream().map(FraudAlert::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FraudAlert> recent = alerts.findByCreatedAtAfter(clock.instant().minus(Duration.ofDays(30)));
        Map<String, Long> reasons = new LinkedHashMap<>();
        recent.forEach(a -> a.reasonCodeList().forEach(code -> reasons.merge(code, 1L, Long::sum)));

        return new Stats(
                accounts.totalBalance(),
                accounts.count(),
                accounts.countByStatus(AccountStatus.FROZEN),
                transactions.countByCreatedAtAfter(LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant()),
                pending.size(),
                pendingAmount,
                tickets.countByStatus(TicketStatus.OPEN),
                tickets.countByStatusAndUrgency(TicketStatus.OPEN, Urgency.URGENT),
                reasons,
                recent.stream().filter(a -> a.getStatus() == FraudAlertStatus.APPROVED).count(),
                recent.stream().filter(a -> a.getStatus() == FraudAlertStatus.REJECTED).count());
    }
}
