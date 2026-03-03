package com.dsi.support.agenticrouter.service.audit;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.AuditEvent;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.AuditEventRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
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
        log.debug(
            "AuditEventRecord({}) SupportTicket(id:{}) AuditEvent(type:{}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            eventType,
            performedById
        );

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

        recordEventWithEntities(eventType, supportTicket, performedBy, description, payload);
    }

    public void recordEventWithEntities(
        AuditEventType eventType,
        SupportTicket supportTicket,
        AppUser performedBy,
        String description,
        JsonNode payload
    ) {
        log.debug(
            "AuditEventRecordWithEntities({}) SupportTicket(id:{}) AuditEvent(type:{}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            eventType,
            Objects.nonNull(performedBy) ? performedBy.getId() : null
        );

        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(supportTicket, "supportTicket");
        Objects.requireNonNull(description, "description");

        String correlationId = MDC.get("correlationId");

        AuditEvent auditEvent = AuditEvent.builder()
                                          .eventType(eventType)
                                          .ticket(supportTicket)
                                          .performedBy(performedBy)
                                          .description(description)
                                          .payload(payload)
                                          .correlationId(correlationId)
                                          .build();

        auditEventRepository.save(auditEvent);

        log.info(
            "AuditEventRecord({}) SupportTicket(id:{}) AuditEvent(id:{},type:{},correlationId:{}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            auditEvent.getId(),
            eventType,
            correlationId,
            Objects.nonNull(performedBy) ? performedBy.getId() : null
        );
    }

    @Transactional(readOnly = true)
    public List<AuditEventRepository.AuditEventView> getTicketAuditTrailView(
        Long ticketId
    ) {
        Objects.requireNonNull(ticketId, "ticketId");

        return auditEventRepository.findTicketAuditTrailView(
            ticketId,
            AuditEventType.getCustomerVisible()
        );
    }

    @Transactional(readOnly = true)
    public boolean hasEventTypeSince(
        Long ticketId,
        AuditEventType eventType,
        Instant since
    ) {
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(since, "since");

        return auditEventRepository.existsByTicket_IdAndEventTypeAndCreatedAtAfter(
            ticketId,
            eventType,
            since
        );
    }
}
