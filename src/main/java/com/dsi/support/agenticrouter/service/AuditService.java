package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.AuditEvent;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.AuditEventRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AppUserRepository appUserRepository;

    public void recordEvent(
        AuditEventType eventType,
        Long ticketId,
        Long performedById,
        String description,
        JsonNode payload
    ) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(description, "description");

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        AppUser performedBy = null;

        if (Objects.nonNull(performedById)) {
            performedBy = appUserRepository.findById(performedById)
                                           .orElseThrow(
                                               DataNotFoundException.supplier(
                                                   AppUser.class,
                                                   performedById
                                               )
                                           );
        }

        AuditEvent auditEvent = AuditEvent.builder()
                                          .eventType(eventType)
                                          .ticket(supportTicket)
                                          .performedBy(performedBy)
                                          .description(description)
                                          .payload(payload)
                                          .build();

        auditEventRepository.save(auditEvent);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getTicketAuditTrail(
        Long ticketId
    ) {
        Objects.requireNonNull(ticketId, "ticketId");

        return auditEventRepository.findByTicket_IdOrderByCreatedAtAsc(
            ticketId
        );
    }

    public void recordTicketAnalysis(
        Long ticketId,
        String section,
        boolean success,
        String errorMessage
    ) {
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(section, "section");

        AuditEventType eventType = AuditEventType.TICKET_ANALYSIS_FAILED;

        if (success) {
            eventType = AuditEventType.TICKET_ANALYSIS_EXECUTED;
        }

        String description = String.format("Ticket section '%s' analysis failed: %s", section, errorMessage);

        if (success) {
            description = String.format("Ticket section '%s' analyzed successfully", section);
        }

        recordEvent(
            eventType,
            ticketId,
            null,
            description,
            null
        );
    }
}
