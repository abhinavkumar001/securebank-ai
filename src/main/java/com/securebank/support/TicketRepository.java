package com.securebank.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findTop50ByStatusOrderByResolvedAtDesc(TicketStatus status);

    long countByStatus(TicketStatus status);

    long countByStatusAndUrgency(TicketStatus status, Urgency urgency);
}
