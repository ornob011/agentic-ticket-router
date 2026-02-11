package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.ArticleTemplate;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.MessageKind;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.knowledge.TemplateService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UseTemplateAction implements TicketAction {

    private final AuditService auditService;
    private final TicketMessageRepository ticketMessageRepository;
    private final TemplateService templateService;

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
    ) {
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

        ActionParams actionParams = new ActionParams(response);

        Long templateId = actionParams.templateId();

        TemplateVariables templateVariables = TemplateVariables.baseForTicket(
            supportTicket
        );

        templateVariables.merge(
            actionParams.variablesExcluding(ActionParamKey.TEMPLATE_ID)
        );

        Long actualTemplateId = Objects.requireNonNullElseGet(
            templateId,
            () -> {
                log.info(
                    "UseTemplateAction({}) SupportTicket(id:{}) Outcome(reason:{})",
                    OperationalLogContext.PHASE_DECISION,
                    supportTicket.getId(),
                    "template_id_missing_auto_select"
                );

                Long selectedTemplateId = autoSelectTemplateId(
                    supportTicket
                );

                if (Objects.isNull(selectedTemplateId)) {
                    log.warn(
                        "UseTemplateAction({}) SupportTicket(id:{},status:{}) Outcome(reason:{})",
                        OperationalLogContext.PHASE_FAIL,
                        supportTicket.getId(),
                        supportTicket.getStatus(),
                        "no_matching_template"
                    );

                    throw new IllegalStateException(
                        "No suitable template found for ticket. Escalating to human review."
                    );
                }

                return selectedTemplateId;
            }
        );

        String filledContent = templateService.fillTemplate(
            actualTemplateId,
            templateVariables.asMap()
        );

        TicketMessage ticketMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .messageKind(MessageKind.AUTO_REPLY)
                                                   .content(filledContent)
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(ticketMessage);

        log.info(
            "UseTemplateAction({}) SupportTicket(id:{},status:{}) Outcome(templateId:{},messageKind:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus(),
            actualTemplateId,
            MessageKind.AUTO_REPLY
        );

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            null,
            "Template used: " + actualTemplateId,
            null
        );

        log.info(
            "UseTemplateAction({}) SupportTicket(id:{},status:{}) Outcome(templateId:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            actualTemplateId
        );
    }

    private Long autoSelectTemplateId(
        SupportTicket supportTicket
    ) {
        Objects.requireNonNull(supportTicket, "supportTicket");

        ArticleTemplate selectedTemplate = templateService.findBestMatchingTemplate(
            supportTicket.getCurrentCategory(),
            supportTicket.getCurrentPriority(),
            supportTicket.getSubject()
        );

        if (Objects.isNull(selectedTemplate)) {
            return null;
        }

        log.info(
            "UseTemplateAutoSelect({}) SupportTicket(id:{},category:{},priority:{}) Template(id:{},name:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getCurrentCategory(),
            supportTicket.getCurrentPriority(),
            selectedTemplate.getId(),
            selectedTemplate.getName()
        );

        auditService.recordEvent(
            AuditEventType.TEMPLATE_AUTO_SELECTED,
            supportTicket.getId(),
            null,
            String.format(
                "Auto-selected template: %s (id=%s) for category=%s, priority=%s",
                selectedTemplate.getName(),
                selectedTemplate.getId(),
                supportTicket.getCurrentCategory(),
                supportTicket.getCurrentPriority()
            ),
            null
        );

        return selectedTemplate.getId();
    }

    private enum ActionParamKey {
        TEMPLATE_ID("template_id");

        private final String key;

        ActionParamKey(
            String key
        ) {
            this.key = key;
        }

        public String key() {
            return key;
        }
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

    private static final class ActionParams {

        private final Map<String, ?> values;

        private ActionParams(
            RouterResponse response
        ) {
            this.values = Objects.requireNonNull(
                response.getActionParameters(),
                "Action parameters are required"
            );
        }

        public Long templateId() {
            String rawTemplateId = text(ActionParamKey.TEMPLATE_ID)
                .orElseThrow(() -> new IllegalStateException(
                    ActionParamKey.TEMPLATE_ID.key() + " is required"
                ));

            if (!StringUtils.isNumeric(rawTemplateId)) {
                throw new IllegalStateException(ActionParamKey.TEMPLATE_ID.key() + " must be numeric");
            }

            return Long.parseLong(rawTemplateId);
        }

        public Map<String, String> variablesExcluding(
            ActionParamKey excludedKey
        ) {
            Map<String, String> variables = new HashMap<>();

            values.forEach((key, value) -> {
                if (excludedKey.key().equals(key)) {
                    return;
                }

                String normalizedValue = StringUtils.trimToNull(Objects.toString(value, null));
                if (normalizedValue == null) {
                    return;
                }

                variables.put(key, normalizedValue);
            });

            return variables;
        }

        private Optional<String> text(
            ActionParamKey actionParamKey
        ) {
            return Optional.ofNullable(values.get(actionParamKey.key()))
                           .map(object -> Objects.toString(object, null))
                           .map(StringUtils::trimToNull);
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
