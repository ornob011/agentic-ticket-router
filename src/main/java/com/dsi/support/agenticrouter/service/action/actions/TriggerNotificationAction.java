package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.NotificationService;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    ) {
        Map<String, Object> params = Objects.requireNonNull(
            routerResponse.getActionParameters(),
            "Action parameters are required"
        );

        String defaultTitle = "Ticket Update: " + supportTicket.getFormattedTicketNo();

        NotificationType type = NOTIFICATION_TYPE_BY_NAME.getOrDefault(
            enumKey(params.get(KEY_NOTIFICATION_TYPE)).orElse(StringUtils.EMPTY),
            DEFAULT_TYPE
        );

        String title = text(params.get(KEY_TITLE)).orElse(defaultTitle);
        String body = text(params.get(KEY_BODY)).orElse(DEFAULT_BODY);

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
    }

    private static Optional<String> text(
        Object value
    ) {
        return Optional.ofNullable(value)
                       .map(Object::toString)
                       .map(StringUtils::trim)
                       .filter(StringUtils::isNotBlank);
    }

    private static Optional<String> enumKey(
        Object value
    ) {
        return text(value).map(String::toUpperCase);
    }
}
