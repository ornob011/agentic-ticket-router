package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketAssignmentCommandService {

    private final SupportTicketRepository supportTicketRepository;
    private final AppUserRepository appUserRepository;
    private final TicketAccessPolicyService ticketAccessPolicyService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public void assignSelf(
        Long ticketId
    ) throws BindException {
        Objects.requireNonNull(ticketId, "ticketId");

        AppUser actor = Utils.getLoggedInUserDetails();
        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        if (!ticketAccessPolicyService.canAssignSelf(
            supportTicket,
            actor
        )) {
            throw BindValidation.fieldError(
                "assignSelfRequest",
                "ticketId",
                "Actor cannot self-assign this ticket"
            );
        }

        AppUser previousAgent = supportTicket.getAssignedAgent();
        supportTicket.setAssignedAgent(actor);
        completeHumanReviewIfSupervisorDecision(
            supportTicket,
            actor
        );
        supportTicket.updateLastActivity();

        if (supportTicket.getStatus() == TicketStatus.ASSIGNED
            || supportTicket.getStatus() == TicketStatus.TRIAGING) {
            supportTicket.setStatus(TicketStatus.IN_PROGRESS);
        }

        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.AGENT_ASSIGNED,
            ticketId,
            actor.getId(),
            String.format(
                "Agent self-assigned: %s (previousAgentId=%s)",
                actor.getFullName(),
                Objects.nonNull(previousAgent) ? previousAgent.getId() : null
            ),
            null
        );
    }

    public void assignAgent(
        Long ticketId,
        Long agentId
    ) throws BindException {
        log.info(
            "AgentAssign({}) SupportTicket(id:{}) Actor(targetAgentId:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            agentId
        );

        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(agentId, "agentId");

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        AppUser agent = appUserRepository.findById(agentId)
                                         .orElseThrow(
                                             DataNotFoundException.supplier(
                                                 AppUser.class,
                                                 agentId
                                             )
                                         );

        AppUser actor = Utils.getLoggedInUserDetails();
        if (!ticketAccessPolicyService.canAssignOthers(actor)) {
            throw BindValidation.fieldError(
                "assignAgentRequest",
                "agentId",
                "Actor cannot assign other agents"
            );
        }

        if (!agent.getRole().equals(UserRole.AGENT)
            && !agent.getRole().equals(UserRole.SUPERVISOR)
            && !agent.getRole().equals(UserRole.ADMIN)) {
            throw BindValidation.fieldError(
                "assignAgentRequest",
                "agentId",
                "Selected user must be an agent, supervisor, or admin."
            );
        }

        AppUser previousAgent = supportTicket.getAssignedAgent();
        supportTicket.setAssignedAgent(agent);
        completeHumanReviewIfSupervisorDecision(
            supportTicket,
            actor
        );
        supportTicket.updateLastActivity();

        if (supportTicket.getStatus() == TicketStatus.ASSIGNED
            || supportTicket.getStatus() == TicketStatus.TRIAGING) {
            supportTicket.setStatus(TicketStatus.IN_PROGRESS);
        }

        supportTicketRepository.save(supportTicket);

        String description = previousAgent != null
            ? String.format("Agent reassigned: %s -> %s", previousAgent.getFullName(), agent.getFullName())
            : String.format("Your ticket has been assigned to %s by your support team", agent.getFullName());

        auditService.recordEvent(
            AuditEventType.AGENT_ASSIGNED,
            ticketId,
            actor.getId(),
            description,
            null
        );

        notificationService.createNotification(
            agent.getId(),
            NotificationType.ASSIGNED_TO_YOU,
            "New Assignment: " + supportTicket.getFormattedTicketNo(),
            "You have been assigned to this ticket.",
            ticketId
        );

        log.info(
            "AgentAssign({}) SupportTicket(id:{},status:{},queue:{}) Actor(agentId:{},role:{}) Outcome(previousAgentId:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            agent.getId(),
            agent.getRole(),
            Objects.nonNull(previousAgent) ? previousAgent.getId() : null
        );
    }

    public void releaseAgent(
        Long ticketId
    ) throws BindException {
        log.info(
            "AgentRelease({}) SupportTicket(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            Utils.getLoggedInUserId()
        );

        Objects.requireNonNull(ticketId, "ticketId");

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        AppUser actor = Utils.getLoggedInUserDetails();
        if (actor.isAgent()
            && Objects.nonNull(supportTicket.getAssignedAgent())
            && !Objects.equals(
            supportTicket.getAssignedAgent().getId(),
            actor.getId()
        )) {
            throw BindValidation.fieldError(
                "releaseAgentRequest",
                "ticketId",
                "Agent can only release self assignment"
            );
        }

        AppUser previousAgent = supportTicket.getAssignedAgent();
        if (previousAgent == null) {
            log.debug(
                "AgentRelease({}) SupportTicket(id:{},status:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                supportTicket.getId(),
                supportTicket.getStatus(),
                "already_unassigned"
            );

            return;
        }

        supportTicket.setAssignedAgent(null);
        if (shouldTransitionToAssignedOnRelease(supportTicket.getStatus())) {
            supportTicket.setStatus(TicketStatus.ASSIGNED);
        }
        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.AGENT_ASSIGNED,
            ticketId,
            Utils.getLoggedInUserId(),
            String.format("Agent released: %s", previousAgent.getFullName()),
            null
        );

        log.info(
            "AgentRelease({}) SupportTicket(id:{},status:{},queue:{}) Outcome(previousAgentId:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            previousAgent.getId()
        );
    }

    private boolean shouldTransitionToAssignedOnRelease(
        TicketStatus currentStatus
    ) {
        return currentStatus == TicketStatus.TRIAGING
               || currentStatus == TicketStatus.IN_PROGRESS
               || currentStatus == TicketStatus.WAITING_CUSTOMER;
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
