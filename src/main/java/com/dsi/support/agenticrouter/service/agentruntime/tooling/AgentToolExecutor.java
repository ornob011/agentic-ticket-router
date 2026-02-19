package com.dsi.support.agenticrouter.service.agentruntime.tooling;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.RoutingActionParameterKey;
import com.dsi.support.agenticrouter.service.agentruntime.tools.TicketActionTools;
import com.dsi.support.agenticrouter.service.agentruntime.tools.ToolExecutionContext;
import com.dsi.support.agenticrouter.util.BindValidation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AgentToolExecutor {

    private static final String ROUTER_RESPONSE_OBJECT = "routerResponse";
    private static final String FIELD_ACTION_PARAMETERS = "actionParameters";

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
        NextAction nextAction = Objects.requireNonNull(
            routerResponse.getNextAction(),
            "routerResponse.nextAction"
        );

        switch (nextAction) {
            case AUTO_REPLY -> ticketActionTools.autoReply(
                requireText(
                    routerResponse.getDraftReply(),
                    "draftReply",
                    "draftReply is required for AUTO_REPLY action"
                )
            );
            case ASK_CLARIFYING -> ticketActionTools.askClarifying(
                requireText(
                    routerResponse.getClarifyingQuestion(),
                    "clarifyingQuestion",
                    "clarifyingQuestion is required for ASK_CLARIFYING action"
                )
            );
            case ASSIGN_QUEUE -> ticketActionTools.assignQueue(
                requireQueue(routerResponse)
            );
            case ESCALATE, AUTO_ESCALATE -> ticketActionTools.escalate(
                requireText(
                    routerResponse.getInternalNote(),
                    "internalNote",
                    "internalNote is required for escalation action"
                )
            );
            case HUMAN_REVIEW -> ticketActionTools.markHumanReview(
                requireText(
                    routerResponse.getInternalNote(),
                    "internalNote",
                    "internalNote is required for HUMAN_REVIEW action"
                )
            );
            case UPDATE_CUSTOMER_PROFILE -> ticketActionTools.updateCustomerProfile(
                readStringParameter(
                    routerResponse,
                    RoutingActionParameterKey.PHONE_NUMBER
                ),
                readStringParameter(
                    routerResponse,
                    RoutingActionParameterKey.COMPANY_NAME
                ),
                readStringParameter(
                    routerResponse,
                    RoutingActionParameterKey.ADDRESS
                ),
                readStringParameter(
                    routerResponse,
                    RoutingActionParameterKey.CITY
                ),
                readStringParameter(
                    routerResponse,
                    RoutingActionParameterKey.POSTAL_CODE
                ),
                readStringParameter(
                    routerResponse,
                    RoutingActionParameterKey.PREFERRED_LANGUAGE_CODE
                )
            );
            case CHANGE_PRIORITY -> ticketActionTools.changePriority(
                requirePriority(routerResponse)
            );
            case ADD_INTERNAL_NOTE -> ticketActionTools.addInternalNote(
                requireText(
                    routerResponse.getInternalNote(),
                    "internalNote",
                    "internalNote is required for ADD_INTERNAL_NOTE action"
                )
            );
            case AUTO_RESOLVE -> ticketActionTools.autoResolve(
                requireText(
                    routerResponse.getDraftReply(),
                    "draftReply",
                    "draftReply is required for AUTO_RESOLVE action"
                )
            );
            case REOPEN_TICKET -> ticketActionTools.reopenTicket(
                requireText(
                    routerResponse.getInternalNote(),
                    "internalNote",
                    "internalNote is required for REOPEN_TICKET action"
                )
            );
            case TRIGGER_NOTIFICATION -> ticketActionTools.triggerNotification(
                readRawParameter(
                    routerResponse,
                    "notification_type"
                ),
                readRawParameter(
                    routerResponse,
                    "title"
                ),
                readRawParameter(
                    routerResponse,
                    "body"
                )
            );
            case USE_KNOWLEDGE_ARTICLE -> ticketActionTools.useKnowledgeArticle(
                requirePositiveLongParameter(
                    routerResponse,
                    RoutingActionParameterKey.ARTICLE_ID
                )
            );
            case USE_TEMPLATE -> ticketActionTools.useTemplate(
                requirePositiveLongParameter(
                    routerResponse,
                    RoutingActionParameterKey.TEMPLATE_ID
                )
            );
        }
    }

    private String requireQueue(
        RouterResponse routerResponse
    ) throws BindException {
        if (routerResponse.getQueue() == null) {
            throw BindValidation.fieldError(
                ROUTER_RESPONSE_OBJECT,
                "queue",
                "queue is required for ASSIGN_QUEUE action"
            );
        }

        return routerResponse.getQueue().name();
    }

    private String requirePriority(
        RouterResponse routerResponse
    ) throws BindException {
        if (routerResponse.getPriority() == null) {
            throw BindValidation.fieldError(
                ROUTER_RESPONSE_OBJECT,
                "priority",
                "priority is required for CHANGE_PRIORITY action"
            );
        }

        return routerResponse.getPriority().name();
    }

    private String requireText(
        String value,
        String fieldName,
        String errorMessage
    ) throws BindException {
        String normalizedValue = StringUtils.trimToNull(
            value
        );

        if (Objects.isNull(normalizedValue)) {
            throw BindValidation.fieldError(
                ROUTER_RESPONSE_OBJECT,
                fieldName,
                errorMessage
            );
        }

        return normalizedValue;
    }

    private long requirePositiveLongParameter(
        RouterResponse routerResponse,
        RoutingActionParameterKey parameterKey
    ) throws BindException {
        String rawValue = readRawParameter(
            routerResponse,
            parameterKey.getKey()
        );

        if (StringUtils.isBlank(rawValue) || !StringUtils.isNumeric(rawValue)) {
            throw BindValidation.fieldError(
                ROUTER_RESPONSE_OBJECT,
                FIELD_ACTION_PARAMETERS,
                parameterKey.getKey() + " must be numeric"
            );
        }

        long parsedValue = Long.parseLong(rawValue);
        if (parsedValue <= 0L) {
            throw BindValidation.fieldError(
                ROUTER_RESPONSE_OBJECT,
                FIELD_ACTION_PARAMETERS,
                parameterKey.getKey() + " must be positive"
            );
        }

        return parsedValue;
    }

    private String readStringParameter(
        RouterResponse routerResponse,
        RoutingActionParameterKey parameterKey
    ) throws BindException {
        return StringUtils.trimToNull(
            readRawParameter(
                routerResponse,
                parameterKey.getKey()
            )
        );
    }

    private String readRawParameter(
        RouterResponse routerResponse,
        String key
    ) throws BindException {
        Map<String, Object> actionParameters = requireActionParameters(
            routerResponse
        );

        Object value = actionParameters.get(
            key
        );

        return Objects.toString(
            value,
            null
        );
    }

    private Map<String, Object> requireActionParameters(
        RouterResponse routerResponse
    ) throws BindException {
        if (Objects.isNull(routerResponse.getActionParameters())) {
            throw BindValidation.fieldError(
                ROUTER_RESPONSE_OBJECT,
                FIELD_ACTION_PARAMETERS,
                "actionParameters are required"
            );
        }

        return routerResponse.getActionParameters();
    }
}
