package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketRouting;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketRoutingRepository;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketRoutingCommandService {

    private final SupportTicketRepository supportTicketRepository;
    private final TicketRoutingRepository ticketRoutingRepository;
    private final TicketAccessPolicyService ticketAccessPolicyService;
    private final AuditService auditService;
    private final TicketWorkflowUpdateService ticketWorkflowUpdateService;
    private final TicketCommandLookupService ticketCommandLookupService;

    public void overrideRouting(
        Long ticketId,
        TicketQueue newQueue,
        TicketPriority newPriority,
        String reason
    ) throws BindException {
        log.info(
            "RoutingOverride({}) SupportTicket(id:{}) Outcome(newQueue:{},newPriority:{},reasonLength:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            newQueue,
            newPriority,
            StringUtils.length(StringNormalizationUtils.trimToNull(reason))
        );

        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(newQueue, "newQueue");
        Objects.requireNonNull(newPriority, "newPriority");

        SupportTicket ticket = ticketCommandLookupService.requireTicket(
            ticketId
        );

        AppUser actor = ticketCommandLookupService.requireCurrentActor();

        if (!ticketAccessPolicyService.canOverrideRouting(actor)) {
            throw BindValidation.fieldError(
                "routingOverrideRequest",
                "ticketId",
                "Actor cannot override routing"
            );
        }

        String normalizedReason = StringNormalizationUtils.trimToNull(reason);
        if (Objects.isNull(normalizedReason)) {
            throw BindValidation.fieldError(
                "routingOverrideRequest",
                "reason",
                "Reason is required"
            );
        }

        Optional<TicketRouting> latestRoutingOptional = ticketRoutingRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
                                                                               .stream()
                                                                               .findFirst();
        if (latestRoutingOptional.isEmpty()) {
            throw BindValidation.fieldError(
                "routingOverrideRequest",
                "ticketId",
                "No routing found for ticket: " + ticketId
            );
        }
        TicketRouting latestRouting = latestRoutingOptional.get();

        Long overriddenById = actor.getId();

        latestRouting.setOverridden(true);
        latestRouting.setOverrideReason(normalizedReason);
        latestRouting.setOverriddenBy(actor);

        ticketRoutingRepository.save(latestRouting);

        ticket.setAssignedQueue(newQueue);
        ticket.setCurrentPriority(newPriority);
        ticketWorkflowUpdateService.completeHumanReviewIfSupervisorDecision(
            ticket,
            actor
        );
        ticket.updateLastActivity();

        supportTicketRepository.save(ticket);

        auditService.recordEvent(
            AuditEventType.ROUTING_OVERRIDDEN,
            ticketId,
            actor.getId(),
            String.format(
                "Routing overridden: queue=%s, priority=%s, reason=%s",
                newQueue,
                newPriority,
                normalizedReason
            ),
            null
        );

        log.info(
            "RoutingOverride({}) SupportTicket(id:{},status:{},queue:{},priority:{}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId(),
            ticket.getStatus(),
            ticket.getAssignedQueue(),
            ticket.getCurrentPriority(),
            overriddenById
        );
    }

}
