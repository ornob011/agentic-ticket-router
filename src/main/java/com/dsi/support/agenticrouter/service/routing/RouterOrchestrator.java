package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.entity.TicketRouting;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.repository.TicketRoutingRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.util.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

// TODO: Fix the class

/**
 * Orchestrates the complete routing pipeline:
 * 1. Call OllamaRouterService for LLM inference
 * 2. Validate response
 * 3. Apply PolicyEngine rules
 * 4. Persist routing decision
 * 5. Delegate to AgenticStateMachine for execution
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RouterOrchestrator {

    private final OllamaRouterService ollamaRouterService;
    private final PolicyEngine policyEngine;
    private final AgenticStateMachine agenticStateMachine;
    private final SupportTicketRepository supportTicketRepository;
    private final TicketRoutingRepository ticketRoutingRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Route a ticket through the complete AI pipeline.
     * This is called asynchronously after ticket creation or customer replies.
     *
     * @param ticket Ticket to route
     */
    @Async
    public void routeTicket(SupportTicket ticket) {
        log.info("Starting routing for ticket: {}", ticket.getFormattedTicketNo());

        try {
            // Transition to TRIAGING
            ticket.setStatus(TicketStatus.TRIAGING);
            ticket = supportTicketRepository.save(ticket);

            // Build routing request context
            RouterRequest request = buildRouterRequest(ticket);

            // Call LLM for routing decision
            RouterResponse response = ollamaRouterService.getRoutingDecision(request, ticket.getId());

            // Apply policy gates (may override response)
            response = policyEngine.applyPolicyGates(response, ticket);

            // Persist routing decision
            int newVersion = ticket.getLatestRoutingVersion() + 1;
            TicketRouting routing = createRoutingRecord(ticket, response, newVersion);
            routing = ticketRoutingRepository.save(routing);

            // Update ticket snapshot fields
            ticket.setCurrentCategory(response.getCategory());
            ticket.setCurrentPriority(response.getPriority());
            ticket.setAssignedQueue(response.getQueue());
            ticket.setLatestRoutingConfidence(response.getConfidence());
            ticket.setLatestRoutingVersion(newVersion);
            ticket = supportTicketRepository.save(ticket);

            // Execute agentic actions based on next_action
            agenticStateMachine.executeAction(ticket, routing);

            log.info("Routing completed for ticket: {} -> queue={}, action={}, confidence={}",
                ticket.getFormattedTicketNo(),
                response.getQueue(),
                response.getNextAction(),
                response.getConfidence());

        } catch (Exception e) {
            log.error("Routing failed for ticket: " + ticket.getFormattedTicketNo(), e);

            // Fall back to human review
            ticket.setStatus(TicketStatus.TRIAGING);
            supportTicketRepository.save(ticket);

            auditService.recordEvent(
                AuditEventType.MODEL_INFERENCE_FAILED,
                ticket.getId(),
                Utils.getLoggedInUserId(),
                "Routing failed: " + e.getMessage(),
                null);
        }
    }

    /**
     * Build context for LLM routing request.
     */
    private RouterRequest buildRouterRequest(SupportTicket ticket) {
        // Get conversation history
        String conversationHistory = ticketMessageRepository
            .findByTicket_IdOrderByCreatedAtAsc(ticket.getId())
            .stream()
            .filter(m -> m.isVisibleToCustomer())
            .map(m -> String.format("[%s] %s: %s",
                m.getCreatedAt(),
                m.getMessageKind(),
                m.getContent()))
            .collect(Collectors.joining("\n"));

        // Get initial message
        String initialMessage = ticketMessageRepository
            .findByTicket_IdOrderByCreatedAtAsc(ticket.getId())
            .stream()
            .findFirst()
            .map(TicketMessage::getContent)
            .orElse("");

        // Get customer tier
        String customerTier = ticket.getCustomer().getCustomerProfile() != null &&
                              ticket.getCustomer().getCustomerProfile().getCustomerTier() != null
            ? ticket.getCustomer().getCustomerProfile().getCustomerTier().getCode()
            : "UNKNOWN";

        return RouterRequest.builder()
                            .ticketId(ticket.getId())
                            .ticketNo(ticket.getFormattedTicketNo())
                            .subject(ticket.getSubject())
                            .customerName(ticket.getCustomer().getFullName())
                            .customerTier(customerTier)
                            .initialMessage(initialMessage)
                            .conversationHistory(conversationHistory)
                            .build();
    }

    /**
     * Create routing record from LLM response.
     */
    private TicketRouting createRoutingRecord(
        SupportTicket ticket,
        RouterResponse response,
        int version
    ) {
        try {
            return TicketRouting.builder()
                                .ticket(ticket)
                                .version(version)
                                .category(response.getCategory())
                                .priority(response.getPriority())
                                .queue(response.getQueue())
                                .nextAction(response.getNextAction())
                                .confidence(response.getConfidence())
                                .clarifyingQuestion(response.getClarifyingQuestion())
                                .draftReply(response.getDraftReply())
                                .rationaleTags(response.getRationaleTags())
                                .overridden(false)
                                .policyGateTriggered(false)
                                .applied(true)
                                .build();
        } catch (Exception e) {
            log.error("Failed to create routing record", e);
            throw new RuntimeException("Failed to persist routing decision", e);
        }
    }
}
