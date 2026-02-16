package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TriggerNotificationAction implements TicketAction {

    private static final String KEY_NOTIFICATION_TYPE = "notification_type";
    private static final String KEY_TITLE = "title";
    private static final String KEY_BODY = "body";

    private static final NotificationType DEFAULT_TYPE = NotificationType.STATUS_CHANGE;
    private static final String DEFAULT_BODY = "There has been an update to your ticket.";

    private static final Map<String, NotificationType> NOTIFICATION_TYPE_BY_NAME =
        Arrays.stream(NotificationType.values())
              .collect(
                  Collectors.toUnmodifiableMap(
                      NotificationType::name,
                      Function.identity()
                  )
              );

    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    private static Optional<String> text(
        Object value
    ) {
        return Optional.ofNullable(value)
                       .map(Object::toString)
                       .map(StringNormalizationUtils::trimToNull)
                       .filter(StringUtils::isNotBlank);
    }

    private static Optional<String> enumKey(
        Object value
    ) {
        return text(value).map(text -> text.toUpperCase(Locale.ROOT));
    }

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.TRIGGER_NOTIFICATION.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) throws BindException {
        log.info(
            "TriggerNotificationAction({}) SupportTicket(id:{},status:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus()
        );

        if (Objects.isNull(routerResponse.getActionParameters())) {
            throw BindValidation.fieldError(
                "routerResponse",
                "actionParameters",
                "actionParameters are required"
            );
        }

        Map<String, Object> params = routerResponse.getActionParameters();

        String defaultTitle = "Ticket Update: " + supportTicket.getFormattedTicketNo();

        NotificationType type = NOTIFICATION_TYPE_BY_NAME.getOrDefault(
            enumKey(params.get(KEY_NOTIFICATION_TYPE)).orElse(StringUtils.EMPTY),
            DEFAULT_TYPE
        );

        String title = text(params.get(KEY_TITLE)).orElse(defaultTitle);
        String body = text(params.get(KEY_BODY)).orElse(DEFAULT_BODY);

        log.info(
            "TriggerNotificationAction({}) SupportTicket(id:{}) Notification(type:{},titleLength:{},bodyLength:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            type,
            StringUtils.length(title),
            StringUtils.length(body)
        );

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            type,
            title,
            body,
            supportTicket.getId()
        );

        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.NOTIFICATION_SENT,
            supportTicket.getId(),
            null,
            "System triggered notification: " + title,
            null
        );

        log.info(
            "TriggerNotificationAction({}) SupportTicket(id:{},status:{}) Notification(type:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            type
        );
    }
}
