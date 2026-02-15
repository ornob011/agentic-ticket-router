package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
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
    private final TicketAccessPolicyService ticketAccessPolicyService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final TicketCommandLookupService ticketCommandLookupService;
    private final TicketAssignmentWorkflowService ticketAssignmentWorkflowService;

    public void assignSelf(
        Long ticketId
    ) throws BindException {
        Objects.requireNonNull(ticketId, "ticketId");

        AppUser actor = ticketCommandLookupService.requireCurrentActor();
        SupportTicket supportTicket = ticketCommandLookupService.requireTicket(
            ticketId
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
        ticketAssignmentWorkflowService.applyAssignment(
            supportTicket,
            actor,
            actor
        );

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

        SupportTicket supportTicket = ticketCommandLookupService.requireTicket(
            ticketId
        );
        AppUser agent = ticketCommandLookupService.requireUser(
            agentId
        );
        AppUser actor = ticketCommandLookupService.requireCurrentActor();
        if (!ticketAccessPolicyService.canAssignOthers(actor)) {
            throw BindValidation.fieldError(
                "assignAgentRequest",
                "agentId",
                "Actor cannot assign other agents"
            );
        }

        if (!agent.canAccessAgentPortal()) {
            throw BindValidation.fieldError(
                "assignAgentRequest",
                "agentId",
                "Selected user must be an agent, supervisor, or admin."
            );
        }

        AppUser previousAgent = supportTicket.getAssignedAgent();
        ticketAssignmentWorkflowService.applyAssignment(
            supportTicket,
            actor,
            agent
        );

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
        AppUser actor = ticketCommandLookupService.requireCurrentActor();

        log.info(
            "AgentRelease({}) SupportTicket(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            actor.getId()
        );

        Objects.requireNonNull(ticketId, "ticketId");

        SupportTicket supportTicket = ticketCommandLookupService.requireTicket(
            ticketId
        );

        if (!actor.isAgent() && !ticketAccessPolicyService.canAssignOthers(actor)) {
            throw BindValidation.fieldError(
                "releaseAgentRequest",
                "ticketId",
                "Actor cannot release agent assignment"
            );
        }

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

        ticketAssignmentWorkflowService.applyRelease(
            supportTicket
        );
        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.AGENT_ASSIGNED,
            ticketId,
            actor.getId(),
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

}
