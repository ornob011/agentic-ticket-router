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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

// TODO: Fix the class

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AppUserRepository appUserRepository;

    public AuditEvent recordEvent(
        AuditEventType eventType,
        Long ticketId,
        Long performedById,
        String description,
        JsonNode payload
    ) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(performedById, "performedById");
        Objects.requireNonNull(description, "description");

        log.debug(
            String.format(
                "Recording audit event: type=%s, ticketId=%s, performedById=%s",
                eventType,
                ticketId,
                performedById
            )
        );

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        AppUser performedBy = appUserRepository.findById(performedById)
                                               .orElseThrow(
                                                   DataNotFoundException.supplier(
                                                       AppUser.class,
                                                       performedById
                                                   )
                                               );

        AuditEvent auditEvent = AuditEvent.builder()
                                          .eventType(eventType)
                                          .ticket(supportTicket)
                                          .performedBy(performedBy)
                                          .description(description)
                                          .payload(payload)
                                          .build();

        return auditEventRepository.save(auditEvent);
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
}
