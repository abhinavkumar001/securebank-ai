package com.securebank.support;

import com.securebank.ai.NaiveBayesClassifier;
import com.securebank.audit.AuditService;
import com.securebank.common.ApiException;
import com.securebank.common.CurrentUser;
import com.securebank.support.dto.TicketResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class SupportService {

    private final TicketRepository tickets;
    private final TicketClassifier classifier;
    private final UrgencyScorer urgencyScorer;
    private final AuditService auditService;

    public SupportService(TicketRepository tickets, TicketClassifier classifier,
                          UrgencyScorer urgencyScorer, AuditService auditService) {
        this.tickets = tickets;
        this.classifier = classifier;
        this.urgencyScorer = urgencyScorer;
        this.auditService = auditService;
    }

    @Transactional
    public TicketResponse raise(CurrentUser user, String message) {
        String text = message.trim();
        NaiveBayesClassifier.Prediction<TicketCategory> prediction = classifier.classify(text);
        UrgencyScorer.Assessment assessment = urgencyScorer.assess(text, prediction.label());
        Ticket ticket = tickets.save(new Ticket(user.id(), text, prediction.label(),
                Math.round(prediction.confidence() * 100) / 100.0, assessment));
        auditService.log(user.id(), "TICKET_RAISED", "Ticket", ticket.getId(),
                prediction.label() + " · " + assessment.urgency());
        return TicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> mine(CurrentUser user) {
        return tickets.findByUserIdOrderByCreatedAtDesc(user.id()).stream().map(TicketResponse::from).toList();
    }

    /** Open tickets in the order an agent should handle them: urgency first, then oldest first. */
    @Transactional(readOnly = true)
    public List<TicketResponse> openQueue() {
        return tickets.findByStatus(TicketStatus.OPEN).stream()
                .sorted(Comparator.comparing(Ticket::getUrgency)
                        .thenComparing(Comparator.comparingDouble(Ticket::getUrgencyScore).reversed())
                        .thenComparing(Ticket::getCreatedAt))
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> resolved() {
        return tickets.findTop50ByStatusOrderByResolvedAtDesc(TicketStatus.RESOLVED).stream()
                .map(TicketResponse::from).toList();
    }

    @Transactional
    public TicketResponse resolve(CurrentUser admin, Long id, String resolution) {
        Ticket ticket = tickets.findById(id).orElseThrow(() -> ApiException.notFound("Ticket not found"));
        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            throw ApiException.conflict("Ticket is already resolved");
        }
        ticket.resolve(admin.id(), resolution.trim());
        auditService.log(admin.id(), "TICKET_RESOLVED", "Ticket", id, resolution.trim());
        return TicketResponse.from(ticket);
    }
}
