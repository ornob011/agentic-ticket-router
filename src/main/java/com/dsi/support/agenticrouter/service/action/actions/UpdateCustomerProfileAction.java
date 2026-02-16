package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.CustomerProfile;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.repository.CustomerProfileRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import com.dsi.support.agenticrouter.service.action.actions.profile.CustomerProfileUpdateOutcome;
import com.dsi.support.agenticrouter.service.action.actions.profile.CustomerProfileUpdateProcessor;
import com.dsi.support.agenticrouter.service.action.actions.profile.CustomerProfileUserMessageRenderer;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateCustomerProfileAction implements TicketAction {

    private final CustomerProfileRepository customerProfileRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final TicketMessageRepository ticketMessageRepository;
    private final CustomerProfileUpdateProcessor customerProfileUpdateProcessor;
    private final CustomerProfileUserMessageRenderer customerProfileUserMessageRenderer;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.UPDATE_CUSTOMER_PROFILE.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) throws BindException {
        log.info(
            "UpdateCustomerProfileAction({}) SupportTicket(id:{},status:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus()
        );

        if (java.util.Objects.isNull(routerResponse.getActionParameters())) {
            throw BindValidation.fieldError(
                "routerResponse",
                "actionParameters",
                "actionParameters are required"
            );
        }

        Map<String, Object> actionParameters = routerResponse.getActionParameters();

        CustomerProfile customerProfile = supportTicket.getCustomer()
                                                       .getCustomerProfile();

        CustomerProfileUpdateOutcome updateOutcome = customerProfileUpdateProcessor.process(
            customerProfile,
            actionParameters
        );

        boolean hasChanges = updateOutcome.changed();

        if (hasChanges) {
            customerProfileRepository.save(customerProfile);
        }

        TicketMessage systemMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .messageKind(MessageKind.SYSTEM_MESSAGE)
                                                   .content(customerProfileUserMessageRenderer.systemMessage(updateOutcome))
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(systemMessage);

        supportTicket.setStatus(TicketStatus.RESOLVED);
        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            null,
            auditMessageContent(updateOutcome),
            null
        );

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            NotificationType.STATUS_CHANGE,
            customerProfileUserMessageRenderer.notificationTitle(updateOutcome),
            customerProfileUserMessageRenderer.notificationBody(updateOutcome),
            supportTicket.getId()
        );

        log.info(
            "UpdateCustomerProfileAction({}) SupportTicket(id:{},status:{}) Outcome(profileChanged:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            hasChanges
        );
    }

    private String auditMessageContent(
        CustomerProfileUpdateOutcome updateOutcome
    ) {
        if (updateOutcome.changed()) {
            return "Customer profile updated: " + updateOutcome.changeSummary();
        }
        return "Customer profile update requested, but no changes were necessary.";
    }
}
