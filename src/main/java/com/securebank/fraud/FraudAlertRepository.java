package com.securebank.fraud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findByStatusOrderByRiskScoreDescCreatedAtAsc(FraudAlertStatus status);

    List<FraudAlert> findTop50ByStatusNotOrderByReviewedAtDesc(FraudAlertStatus status);

    List<FraudAlert> findByCreatedAtAfter(Instant after);

    Optional<FraudAlert> findByTransactionReference(String reference);

    long countByStatus(FraudAlertStatus status);
}
