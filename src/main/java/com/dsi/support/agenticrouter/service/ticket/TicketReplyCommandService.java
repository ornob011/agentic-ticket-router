package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.MessageKind;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.event.CategoryDetectionEvent;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketReplyCommandService {

    private static final String BUSINESS_DRIVER_AGENT_REPLY = "Agent Reply";

    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TicketAccessPolicyService ticketAccessPolicyService;
    private final CustomerReplyLifecycleService customerReplyLifecycleService;
    private final TicketCommandLookupService ticketCommandLookupService;
    private final AgentReplyWorkflowService agentReplyWorkflowService;

    public void addCustomerReply(
        Long ticketId,
        String content,
        Long customerId
    ) throws BindException {
        String normalizedContent = StringNormalizationUtils.trimToNull(content);

        log.info(
            "CustomerReply({}) SupportTicket(id:{}) Actor(id:{}) Outcome(contentLength:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            customerId,
            StringUtils.length(normalizedContent)
        );

        AppUser customer = ticketCommandLookupService.requireUser(
            customerId
        );
        SupportTicket supportTicket = ticketCommandLookupService.requireTicket(
            ticketId
        );

        if (!ticketAccessPolicyService.canReply(
            supportTicket,
            customer
        )) {
            throw BindValidation.fieldError(
                "ticketReplyRequest",
                "ticketId",
                "Actor cannot reply to this ticket"
            );
        }

        if (Objects.isNull(normalizedContent)) {
            log.warn(
                "CustomerReply({}) SupportTicket(id:{}) Actor(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                supportTicket.getId(),
                customerId,
                "empty_content"
            );

            return;
        }

        if (Objects.nonNull(supportTicket.getStatus())
            && supportTicket.getStatus().isClosedForReplies()
        ) {
            log.info(
                "CustomerReply({}) SupportTicket(id:{},status:{},queue:{},priority:{}) Outcome(reason:{},currentCategory:{})",
                OperationalLogContext.PHASE_DECISION,
                supportTicket.getId(),
                supportTicket.getStatus(),
                supportTicket.getAssignedQueue(),
                supportTicket.getCurrentPriority(),
                "closed_for_reply_with_category_detection",
                supportTicket.getCurrentCategory()
            );

            TicketMessage customerMessage = TicketMessage.builder()
                                                         .ticket(supportTicket)
                                                         .content(normalizedContent)
                                                         .messageKind(MessageKind.CUSTOMER_MESSAGE)
                                                         .author(customer)
                                                         .visibleToCustomer(true)
                                                         .build();

            ticketMessageRepository.save(customerMessage);

            eventPublisher.publishEvent(
                new CategoryDetectionEvent(
                    this,
                    ticketId,
                    normalizedContent,
                    customerId
                )
            );

            log.info(
                "CustomerReply({}) SupportTicket(id:{},status:{}) Outcome(event:{})",
                OperationalLogContext.PHASE_COMPLETE,
                supportTicket.getId(),
                supportTicket.getStatus(),
                CategoryDetectionEvent.class.getSimpleName()
            );

            return;
        }

        TicketMessage ticketMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .author(customer)
                                                   .messageKind(MessageKind.CUSTOMER_MESSAGE)
                                                   .content(normalizedContent)
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(ticketMessage);

        supportTicket.updateLastActivity();

        customerReplyLifecycleService.handleCustomerReply(
            supportTicket,
            customer,
            this
        );

        supportTicketRepository.save(supportTicket);

        log.info(
            "CustomerReply({}) SupportTicket(id:{},status:{},queue:{},priority:{}) Outcome(handlerStatus:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            supportTicket.getCurrentPriority(),
            supportTicket.getStatus()
        );
    }

    public void addAgentReply(
        Long ticketId,
        String content
    ) throws BindException {
        Long agentId = Utils.getLoggedInUserId();

        AppUser agent = ticketCommandLookupService.requireUser(
            agentId
        );

        addAgentReply(
            ticketId,
            content,
            agent,
            BUSINESS_DRIVER_AGENT_REPLY
        );
    }

    public void addAgentReply(
        Long ticketId,
        String content,
        AppUser agent,
        String businessDriver
    ) throws BindException {
        log.info(
            "AgentReply({}) SupportTicket(id:{}) Actor(id:{},role:{}) Outcome(contentLength:{},businessDriver:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            OperationalLogContext.actorId(agent),
            OperationalLogContext.actorRole(agent),
            StringUtils.length(StringNormalizationUtils.trimToNull(content)),
            businessDriver
        );

        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(agent, "agent");

        SupportTicket supportTicket = ticketCommandLookupService.requireTicket(
            ticketId
        );

        if (!ticketAccessPolicyService.canReply(
            supportTicket,
            agent
        )) {
            throw BindValidation.fieldError(
                "ticketReplyRequest",
                "ticketId",
                "Actor cannot reply to this ticket"
            );
        }

        String normalizedContent = StringNormalizationUtils.trimToNull(content);
        if (Objects.isNull(normalizedContent)) {
            log.warn(
                "AgentReply({}) SupportTicket(id:{}) Actor(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                ticketId,
                OperationalLogContext.actorId(agent),
                "empty_content"
            );

            return;
        }

        TicketMessage ticketMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .author(agent)
                                                   .messageKind(MessageKind.AGENT_MESSAGE)
                                                   .content(normalizedContent)
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(ticketMessage);

        supportTicket.updateLastActivity();
        agentReplyWorkflowService.handleAgentReply(
            supportTicket,
            agent,
            businessDriver
        );

        supportTicketRepository.save(supportTicket);

        log.info(
            "AgentReply({}) SupportTicket(id:{},status:{},queue:{}) Actor(id:{}) Outcome(messageKind:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            agent.getId(),
            MessageKind.AGENT_MESSAGE
        );
    }

}
