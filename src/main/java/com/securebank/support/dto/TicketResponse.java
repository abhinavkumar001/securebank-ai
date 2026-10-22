package com.securebank.support.dto;

import com.securebank.support.Ticket;

import java.time.Instant;

public record TicketResponse(Long id, Long userId, String message, String category, String categoryLabel,
                             double categoryConfidence, String urgency, double urgencyScore, String tone,
                             String status, String resolution, Instant createdAt, Instant resolvedAt) {

    public static TicketResponse from(Ticket t) {
        return new TicketResponse(t.getId(), t.getUserId(), t.getMessage(), t.getCategory().name(),
                t.getCategory().label(), t.getCategoryConfidence(), t.getUrgency().name(), t.getUrgencyScore(),
                t.getTone(), t.getStatus().name(), t.getResolution(), t.getCreatedAt(), t.getResolvedAt());
    }
}
