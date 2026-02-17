package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgentReplyWorkflowService {

    private static final String BUSINESS_DRIVER_UNSPECIFIED = "System";

    private final TicketWorkflowUpdateService ticketWorkflowUpdateService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public void handleAgentReply(
        SupportTicket supportTicket,
        AppUser agent,
        String businessDriver
    ) {
        ticketWorkflowUpdateService.completeHumanReviewIfSupervisorDecision(
            supportTicket,
            agent
        );

        String normalizedBusinessDriver = Optional.ofNullable(businessDriver)
                                                  .map(String::trim)
                                                  .filter(StringUtils::isNotBlank)
                                                  .orElse(BUSINESS_DRIVER_UNSPECIFIED);

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            agent.getId(),
            String.format(
                "Agent replied to customer (%s).",
                normalizedBusinessDriver
            ),
            null
        );

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            NotificationType.NEW_MESSAGE,
            String.format("New Reply: %s", supportTicket.getFormattedTicketNo()),
            "An agent has replied to your ticket.",
            supportTicket.getId()
        );
    }
}
