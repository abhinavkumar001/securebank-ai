package com.securebank.fraud;

import com.securebank.common.CurrentUser;
import com.securebank.transaction.TransactionService;
import com.securebank.transaction.dto.TransactionResponse;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin-only (the JwtInterceptor guards /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
public class ReviewController {

    public record ReviewDecisionRequest(@Size(max = 300) String note) {}

    private final FraudAlertRepository alerts;
    private final TransactionService transactionService;
    private final FraudModel fraudModel;

    public ReviewController(FraudAlertRepository alerts, TransactionService transactionService, FraudModel fraudModel) {
        this.alerts = alerts;
        this.transactionService = transactionService;
        this.fraudModel = fraudModel;
    }

    /** Pending items, riskiest first. */
    @GetMapping("/reviews")
    public List<ReviewResponse> pending() {
        return alerts.findByStatusOrderByRiskScoreDescCreatedAtAsc(FraudAlertStatus.PENDING).stream()
                .map(ReviewResponse::from).toList();
    }

    /** Recently decided items. */
    @GetMapping("/reviews/history")
    public List<ReviewResponse> history() {
        return alerts.findTop50ByStatusNotOrderByReviewedAtDesc(FraudAlertStatus.PENDING).stream()
                .map(ReviewResponse::from).toList();
    }

    @PostMapping("/reviews/{id}/approve")
    public TransactionResponse approve(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser admin,
                                       @PathVariable Long id,
                                       @RequestBody(required = false) ReviewDecisionRequest body) {
        return transactionService.approveHeld(admin, id, body == null ? null : body.note());
    }

    @PostMapping("/reviews/{id}/reject")
    public TransactionResponse reject(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser admin,
                                      @PathVariable Long id,
                                      @RequestBody(required = false) ReviewDecisionRequest body) {
        return transactionService.rejectHeld(admin, id, body == null ? null : body.note());
    }

    @GetMapping("/fraud/model")
    public ModelMetrics model() {
        return fraudModel.metrics();
    }
}
