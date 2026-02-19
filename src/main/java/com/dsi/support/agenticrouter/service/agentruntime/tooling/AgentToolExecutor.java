package com.dsi.support.agenticrouter.service.agentruntime.tooling;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.service.agentruntime.tools.TicketActionTools;
import com.dsi.support.agenticrouter.service.agentruntime.tools.ToolExecutionContext;
import com.dsi.support.agenticrouter.util.BindValidation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;

@Service
@RequiredArgsConstructor
public class AgentToolExecutor {

    private final TicketActionTools ticketActionTools;

    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) throws BindException {
        ToolExecutionContext.init(
            supportTicket
        );

        try {
            executeToolByAction(
                routerResponse
            );
        } finally {
            ToolExecutionContext.clear();
        }
    }

    private void executeToolByAction(
        RouterResponse routerResponse
    ) throws BindException {
        NextAction nextAction = routerResponse.getNextAction();

        switch (nextAction) {
            case AUTO_REPLY -> ticketActionTools.autoReply(
                resolveReplyContent(routerResponse)
            );
            case ASK_CLARIFYING -> ticketActionTools.askClarifying(
                resolveClarifyingQuestion(routerResponse)
            );
            case ASSIGN_QUEUE -> ticketActionTools.assignQueue(
                requireQueue(routerResponse)
            );
            case ESCALATE, AUTO_ESCALATE -> ticketActionTools.escalate(
                resolveReason(routerResponse)
            );
            case HUMAN_REVIEW -> ticketActionTools.markHumanReview(
                resolveReason(routerResponse)
            );
            case UPDATE_CUSTOMER_PROFILE, ADD_INTERNAL_NOTE -> ticketActionTools.addInternalNote(
                resolveReason(routerResponse)
            );
            case CHANGE_PRIORITY -> ticketActionTools.changePriority(
                requirePriority(routerResponse)
            );
            case AUTO_RESOLVE, USE_KNOWLEDGE_ARTICLE, USE_TEMPLATE -> ticketActionTools.autoResolve(
                resolveReplyContent(routerResponse)
            );
            case REOPEN_TICKET -> ticketActionTools.reopenTicket(
                resolveReason(routerResponse)
            );
            case TRIGGER_NOTIFICATION -> ticketActionTools.triggerNotification(
                NotificationType.STATUS_CHANGE.name(),
                "Ticket Update",
                resolveReason(routerResponse)
            );
        }
    }

    private String requireQueue(
        RouterResponse routerResponse
    ) throws BindException {
        if (routerResponse.getQueue() == null) {
            throw BindValidation.fieldError(
                "routerResponse",
                "queue",
                "Queue is required for ASSIGN_QUEUE action"
            );
        }

        return routerResponse.getQueue().name();
    }

    private String requirePriority(
        RouterResponse routerResponse
    ) throws BindException {
        if (routerResponse.getPriority() == null) {
            throw BindValidation.fieldError(
                "routerResponse",
                "priority",
                "Priority is required for CHANGE_PRIORITY action"
            );
        }

        return routerResponse.getPriority().name();
    }

    private String resolveClarifyingQuestion(
        RouterResponse routerResponse
    ) {
        return StringUtils.defaultIfBlank(
            routerResponse.getClarifyingQuestion(),
            "Could you provide more details so I can help you better?"
        );
    }

    private String resolveReplyContent(
        RouterResponse routerResponse
    ) {
        return StringUtils.defaultIfBlank(
            routerResponse.getDraftReply(),
            "Thank you for your message. We have processed your request."
        );
    }

    private String resolveReason(
        RouterResponse routerResponse
    ) {
        return StringUtils.defaultIfBlank(
            routerResponse.getInternalNote(),
            "Action triggered by agent runtime policy."
        );
    }
}
