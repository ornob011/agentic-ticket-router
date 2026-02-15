package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketRouting;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketRoutingRepository;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketRoutingCommandService {

    private final SupportTicketRepository supportTicketRepository;
    private final TicketRoutingRepository ticketRoutingRepository;
    private final AppUserRepository appUserRepository;
    private final TicketAccessPolicyService ticketAccessPolicyService;
    private final AuditService auditService;

    public void overrideRouting(
        Long ticketId,
        TicketQueue newQueue,
        TicketPriority newPriority,
        String reason
    ) {
        log.info(
            "RoutingOverride({}) SupportTicket(id:{}) Outcome(newQueue:{},newPriority:{},reasonLength:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            newQueue,
            newPriority,
            StringUtils.length(StringUtils.trimToNull(reason))
        );

        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(newQueue, "newQueue");
        Objects.requireNonNull(newPriority, "newPriority");

        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                                                      .orElseThrow(
                                                          DataNotFoundException.supplier(
                                                              SupportTicket.class,
                                                              ticketId
                                                          )
                                                      );

        AppUser actor = Utils.getLoggedInUserDetails();

        if (!ticketAccessPolicyService.canOverrideRouting(actor)) {
            throw new IllegalStateException("Actor cannot override routing");
        }

        TicketRouting latestRouting = ticketRoutingRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
                                                             .stream()
                                                             .findFirst()
                                                             .orElseThrow(
                                                                 () -> new IllegalStateException("No routing found for ticket: " + ticketId)
                                                             );

        Long overriddenById = Utils.getLoggedInUserId();

        AppUser overriddenBy = appUserRepository.findById(overriddenById)
                                                .orElseThrow(
                                                    DataNotFoundException.supplier(
                                                        AppUser.class,
                                                        overriddenById
                                                    )
                                                );

        latestRouting.setOverridden(true);
        latestRouting.setOverrideReason(reason);
        latestRouting.setOverriddenBy(overriddenBy);

        ticketRoutingRepository.save(latestRouting);

        ticket.setAssignedQueue(newQueue);
        ticket.setCurrentPriority(newPriority);
        completeHumanReviewIfSupervisorDecision(
            ticket,
            actor
        );
        ticket.updateLastActivity();

        supportTicketRepository.save(ticket);

        auditService.recordEvent(
            AuditEventType.ROUTING_OVERRIDDEN,
            ticketId,
            Utils.getLoggedInUserId(),
            String.format(
                "Routing overridden: queue=%s, priority=%s, reason=%s",
                newQueue,
                newPriority,
                reason
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

    private void completeHumanReviewIfSupervisorDecision(
        SupportTicket supportTicket,
        AppUser actor
    ) {
        if (Objects.nonNull(actor) && (actor.isSupervisor() || actor.isAdmin())) {
            supportTicket.setRequiresHumanReview(false);
        }
    }
}
