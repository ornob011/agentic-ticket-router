package com.dsi.support.agenticrouter.service.action.handlers;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.MessageKind;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.action.ActionParameterService;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.knowledge.TemplateService;
import com.dsi.support.agenticrouter.service.memory.MemoryContextService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class UseTemplateAction implements TicketAction {

    private final AuditService auditService;
    private final TicketMessageRepository ticketMessageRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final TemplateService templateService;
    private final ActionParameterService actionParameterService;
    private final MemoryContextService memoryContextService;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.USE_TEMPLATE.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse response
    ) throws BindException {
        log.info(
            "UseTemplateAction({}) SupportTicket(id:{},status:{},queue:{},priority:{}) RouterResponse(confidence:{},actionParamCount:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            supportTicket.getCurrentPriority(),
            response.getConfidence(),
            Objects.nonNull(response.getActionParameters()) ? response.getActionParameters().size() : 0
        );

        Map<String, ?> actionParameters = actionParameterService.requireParameters(
            response.getActionParameters(),
            "routerResponse"
        );
        Long templateId = actionParameterService.requireNumericLong(
            actionParameters,
            "routerResponse",
            ActionParamKey.TEMPLATE_ID
        );

        TemplateVariables templateVariables = TemplateVariables.baseForTicket(
            supportTicket
        );

        templateVariables.merge(
            actionParameterService.stringValuesExcluding(
                actionParameters,
                Set.of(ActionParamKey.TEMPLATE_ID)
            )
        );

        String filledContent = templateService.fillTemplate(
            templateId,
            templateVariables.asMap()
        );

        TicketMessage ticketMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .messageKind(MessageKind.AUTO_REPLY)
                                                   .content(filledContent)
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(ticketMessage);
        memoryContextService.appendAssistantMessage(
            supportTicket,
            filledContent
        );
        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        log.info(
            "UseTemplateAction({}) SupportTicket(id:{},status:{}) Outcome(templateId:{},messageKind:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus(),
            templateId,
            MessageKind.AUTO_REPLY
        );

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            null,
            "Template used: " + templateId,
            null
        );

        log.info(
            "UseTemplateAction({}) SupportTicket(id:{},status:{}) Outcome(templateId:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            templateId
        );
    }

    private enum TemplateVariableKey {
        CUSTOMER_NAME("customer_name"),
        TICKET_NO("ticket_no");

        private final String key;

        TemplateVariableKey(
            String key
        ) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private static final class ActionParamKey {
        private static final String TEMPLATE_ID = "template_id";

        private ActionParamKey() {
        }
    }

    private static final class TemplateVariables {

        private final Map<String, String> variables = new HashMap<>();

        public static TemplateVariables baseForTicket(
            SupportTicket ticket
        ) {
            TemplateVariables templateVariables = new TemplateVariables();

            templateVariables.put(TemplateVariableKey.CUSTOMER_NAME, ticket.getCustomer().getFullName());
            templateVariables.put(TemplateVariableKey.TICKET_NO, ticket.getFormattedTicketNo());

            return templateVariables;
        }

        public void merge(
            Map<String, String> otherVariables
        ) {
            variables.putAll(otherVariables);
        }

        public Map<String, String> asMap() {
            return variables;
        }

        private void put(
            TemplateVariableKey key,
            String value
        ) {
            variables.put(key.key(), value);
        }
    }
}
