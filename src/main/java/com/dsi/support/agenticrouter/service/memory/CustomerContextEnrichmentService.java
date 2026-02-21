package com.dsi.support.agenticrouter.service.memory;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerContextEnrichmentService {

    private static final int MAX_CONTEXT_MESSAGES = 10;
    private static final int MAX_RECENT_TICKETS = 5;

    private final SupportTicketRepository supportTicketRepository;
    private final MemoryContextService memoryContextService;

    @Transactional(readOnly = true)
    public String buildCustomerContext(
        Long customerId,
        Long currentTicketId
    ) {
        log.debug(
            "CustomerContextEnrichment({}) Customer(id:{}) SupportTicket(id:{})",
            OperationalLogContext.PHASE_START,
            customerId,
            currentTicketId
        );

        StringBuilder contextBuilder = new StringBuilder();

        String memoryContext = buildMemoryContext(
            customerId
        );

        if (StringUtils.isNotBlank(memoryContext)) {
            contextBuilder.append("## Customer Conversation History\n");
            contextBuilder.append(memoryContext);
            contextBuilder.append("\n");
        }

        String ticketHistoryContext = buildRecentTicketHistory(
            customerId,
            currentTicketId
        );

        if (StringUtils.isNotBlank(ticketHistoryContext)) {
            contextBuilder.append("## Recent Resolved Tickets\n");
            contextBuilder.append(ticketHistoryContext);
        }

        String result = contextBuilder.toString();

        log.debug(
            "CustomerContextEnrichment({}) Customer(id:{}) SupportTicket(id:{}) Outcome(contextLength:{},memorySection:{},ticketSection:{})",
            OperationalLogContext.PHASE_COMPLETE,
            customerId,
            currentTicketId,
            result.length(),
            StringUtils.isNotBlank(memoryContext),
            StringUtils.isNotBlank(ticketHistoryContext)
        );

        return result;
    }

    private String buildMemoryContext(
        Long customerId
    ) {
        List<Message> messages = memoryContextService.getCustomerContext(
            customerId
        );

        if (messages.isEmpty()) {
            log.debug(
                "CustomerContextMemoryBuild({}) customerId:{} Outcome(reason:{})",
                OperationalLogContext.PHASE_DECISION,
                customerId,
                "no_messages"
            );

            return StringUtils.EMPTY;
        }

        int messageCount = Math.min(
            messages.size(),
            MAX_CONTEXT_MESSAGES
        );

        List<Message> recentMessages = messages.subList(
            Math.max(
                0,
                messages.size() - messageCount
            ),
            messages.size()
        );

        log.debug(
            "CustomerContextMemoryBuild({}) Customer(id:{}) Outcome(totalMessages:{},includedMessages:{})",
            OperationalLogContext.PHASE_DECISION,
            customerId,
            messages.size(),
            recentMessages.size()
        );

        return recentMessages.stream()
                             .map(this::formatMessage)
                             .collect(Collectors.joining("\n"));
    }

    private String buildRecentTicketHistory(
        Long customerId,
        Long currentTicketId
    ) {
        List<SupportTicket> recentTickets = supportTicketRepository.findByCustomerIdAndIdNotOrderByCreatedAtDesc(
            customerId,
            currentTicketId
        );

        if (recentTickets.isEmpty()) {
            log.debug(
                "CustomerContextTicketHistoryBuild({}) Customer(id:{}) SupportTicket(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_DECISION,
                customerId,
                currentTicketId,
                "no_recent_tickets"
            );

            return StringUtils.EMPTY;
        }

        List<SupportTicket> includedTickets = recentTickets.stream()
                                                           .limit(MAX_RECENT_TICKETS)
                                                           .filter(ticket -> ticket.getCurrentCategory() != null)
                                                           .toList();

        log.debug(
            "CustomerContextTicketHistoryBuild({}) Customer(id:{}) SupportTicket(id:{}) Outcome(totalTickets:{},includedTickets:{})",
            OperationalLogContext.PHASE_DECISION,
            customerId,
            currentTicketId,
            recentTickets.size(),
            includedTickets.size()
        );

        return includedTickets.stream()
                              .map(this::formatTicketSummary)
                              .collect(Collectors.joining("\n"));
    }

    private String formatMessage(
        Message message
    ) {
        String role = switch (message.getMessageType()) {
            case USER -> "Customer";
            case ASSISTANT -> "Agent";
            default -> "System";
        };

        return String.format(
            "[%s]: %s",
            role,
            StringUtils.abbreviate(message.getText(), 500)
        );
    }

    private String formatTicketSummary(
        SupportTicket ticket
    ) {
        return String.format(
            "- Ticket %s: %s (Category: %s, Priority: %s, Status: %s)",
            ticket.getTicketNo(),
            StringUtils.abbreviate(ticket.getSubject(), 50),
            ticket.getCurrentCategory(),
            ticket.getCurrentPriority(),
            ticket.getStatus()
        );
    }
}
