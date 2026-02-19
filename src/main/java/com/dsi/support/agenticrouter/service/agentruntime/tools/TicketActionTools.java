package com.dsi.support.agenticrouter.service.agentruntime.tools;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.agentruntime.tooling.RuntimeActionAdapterService;
import com.dsi.support.agenticrouter.service.knowledge.KnowledgeBaseVectorStore;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketActionTools {

    private static final int DEFAULT_HISTORY_LIMIT = 5;
    private static final int MAX_HISTORY_LIMIT = 10;
    private static final int DEFAULT_SEARCH_TOP_K = 3;
    private static final int MAX_SEARCH_TOP_K = 10;
    private static final double TOOL_SEARCH_SIMILARITY_THRESHOLD = 0.75D;

    private static final String KEY_NOTIFICATION_TYPE = "notification_type";
    private static final String KEY_TITLE = "title";
    private static final String KEY_BODY = "body";

    private final RuntimeActionAdapterService runtimeActionAdapterService;
    private final SupportTicketRepository supportTicketRepository;
    private final KnowledgeBaseVectorStore knowledgeBaseVectorStore;

    @Tool(description = "Assign ticket to a queue for agent handling")
    public void assignQueue(
        @ToolParam(description = "The queue code: BILLING_Q, TECH_Q, OPS_Q, SECURITY_Q, ACCOUNT_Q, GENERAL_Q") String queue
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.ASSIGN_QUEUE
        );
        routerResponse.setQueue(
            TicketQueue.valueOf(
                queue
            )
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Escalate ticket to supervisor or human review workflow")
    public void escalate(
        @ToolParam(description = "Escalation reason") String reason
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.ESCALATE
        );
        routerResponse.setInternalNote(
            reason
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Send an automated reply to the customer and resolve the ticket")
    public void autoReply(
        @ToolParam(description = "The reply content to send to the customer") String content
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.AUTO_REPLY
        );
        routerResponse.setDraftReply(
            content
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Request clarifying information from the customer")
    public void askClarifying(
        @ToolParam(description = "The clarifying question to ask the customer") String question
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.ASK_CLARIFYING
        );
        routerResponse.setClarifyingQuestion(
            question
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Change the ticket priority level")
    public void changePriority(
        @ToolParam(description = "The new priority level: LOW, MEDIUM, HIGH, or URGENT") String priority
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.CHANGE_PRIORITY
        );
        routerResponse.setPriority(
            TicketPriority.valueOf(
                priority
            )
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Add an internal note visible only to agents")
    public void addInternalNote(
        @ToolParam(description = "The internal note content") String note
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.ADD_INTERNAL_NOTE
        );
        routerResponse.setInternalNote(
            note
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Mark the ticket for human review")
    public void markHumanReview(
        @ToolParam(description = "The reason for requiring human review") String reason
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.HUMAN_REVIEW
        );
        routerResponse.setInternalNote(
            reason
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Auto-resolve the ticket with a solution")
    public void autoResolve(
        @ToolParam(description = "The solution content to send to the customer") String solution
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.AUTO_RESOLVE
        );
        routerResponse.setDraftReply(
            solution
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Use knowledge base article for resolution")
    public void useKnowledgeArticle(
        @ToolParam(description = "Knowledge article id") long articleId
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        Map<String, Object> actionParameters = new HashMap<>();
        actionParameters.put(
            RoutingActionParameterKey.ARTICLE_ID.getKey(),
            articleId
        );

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.USE_KNOWLEDGE_ARTICLE
        );
        routerResponse.setActionParameters(
            actionParameters
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Use response template for resolution")
    public void useTemplate(
        @ToolParam(description = "Template id") long templateId
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        Map<String, Object> actionParameters = new HashMap<>();
        actionParameters.put(
            RoutingActionParameterKey.TEMPLATE_ID.getKey(),
            templateId
        );

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.USE_TEMPLATE
        );
        routerResponse.setActionParameters(
            actionParameters
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Update customer profile information")
    public void updateCustomerProfile(
        @ToolParam(description = "Phone number") String phoneNumber,
        @ToolParam(description = "Company name") String companyName,
        @ToolParam(description = "Address") String address,
        @ToolParam(description = "City") String city,
        @ToolParam(description = "Postal code") String postalCode,
        @ToolParam(description = "Preferred language code") String preferredLanguageCode
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        Map<String, Object> actionParameters = new HashMap<>();
        putIfNotBlank(
            actionParameters,
            RoutingActionParameterKey.PHONE_NUMBER.getKey(),
            phoneNumber
        );
        putIfNotBlank(
            actionParameters,
            RoutingActionParameterKey.COMPANY_NAME.getKey(),
            companyName
        );
        putIfNotBlank(
            actionParameters,
            RoutingActionParameterKey.ADDRESS.getKey(),
            address
        );
        putIfNotBlank(
            actionParameters,
            RoutingActionParameterKey.CITY.getKey(),
            city
        );
        putIfNotBlank(
            actionParameters,
            RoutingActionParameterKey.POSTAL_CODE.getKey(),
            postalCode
        );
        putIfNotBlank(
            actionParameters,
            RoutingActionParameterKey.PREFERRED_LANGUAGE_CODE.getKey(),
            preferredLanguageCode
        );

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.UPDATE_CUSTOMER_PROFILE
        );
        routerResponse.setActionParameters(
            actionParameters
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Reopen a previously resolved or closed ticket")
    public void reopenTicket(
        @ToolParam(description = "Reason for reopening") String reason
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.REOPEN_TICKET
        );
        routerResponse.setInternalNote(
            reason
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Send a notification to relevant parties")
    public void triggerNotification(
        @ToolParam(description = "Notification type: STATUS_CHANGE, NEW_MESSAGE, or ESCALATION") String notificationType,
        @ToolParam(description = "Notification title") String title,
        @ToolParam(description = "Notification body") String body
    ) throws BindException {
        SupportTicket ticket = currentTicket();

        Map<String, Object> actionParameters = new HashMap<>();
        putIfNotBlank(
            actionParameters,
            KEY_NOTIFICATION_TYPE,
            notificationType
        );
        putIfNotBlank(
            actionParameters,
            KEY_TITLE,
            title
        );
        putIfNotBlank(
            actionParameters,
            KEY_BODY,
            body
        );

        RouterResponse routerResponse = baseResponse(
            ticket,
            NextAction.TRIGGER_NOTIFICATION
        );
        routerResponse.setActionParameters(
            actionParameters
        );

        executeAction(
            ticket,
            routerResponse
        );
    }

    @Tool(description = "Retrieve current ticket information")
    public String getTicketInfo() {
        SupportTicket ticket = currentTicket();

        return String.format(
            "Ticket %s | Subject: %s | Status: %s | Queue: %s | Priority: %s | Category: %s | HumanReview: %s",
            ticket.getFormattedTicketNo(),
            ticket.getSubject(),
            ticket.getStatus(),
            ticket.getAssignedQueue(),
            ticket.getCurrentPriority(),
            ticket.getCurrentCategory(),
            ticket.isRequiresHumanReview()
        );
    }

    @Tool(description = "Retrieve customer ticket history")
    public String getCustomerHistory(
        @ToolParam(description = "Maximum number of tickets to return") Integer limit
    ) {
        SupportTicket ticket = currentTicket();
        int resolvedLimit = normalizeLimit(
            limit,
            DEFAULT_HISTORY_LIMIT,
            MAX_HISTORY_LIMIT
        );

        List<SupportTicket> tickets = supportTicketRepository.findTop5ByCustomerIdOrderByLastActivityAtDesc(
            ticket.getCustomer().getId()
        );

        return tickets.stream()
                      .limit(resolvedLimit)
                      .map(historyTicket -> String.format(
                          "%s | %s | %s | %s",
                          historyTicket.getFormattedTicketNo(),
                          historyTicket.getStatus(),
                          historyTicket.getCurrentPriority(),
                          StringUtils.abbreviate(historyTicket.getSubject(), 80)
                      ))
                      .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Search knowledge base for relevant articles")
    public String searchKnowledgeBase(
        @ToolParam(description = "Knowledge base search query") String query,
        @ToolParam(description = "Top K results to return") Integer topK
    ) {
        int resolvedTopK = normalizeLimit(
            topK,
            DEFAULT_SEARCH_TOP_K,
            MAX_SEARCH_TOP_K
        );

        List<Document> documents = knowledgeBaseVectorStore.searchSimilar(
            query,
            resolvedTopK,
            TOOL_SEARCH_SIMILARITY_THRESHOLD
        );

        if (documents.isEmpty()) {
            return "No matching articles found.";
        }

        return documents.stream()
                        .map(this::formatKnowledgeDocument)
                        .collect(Collectors.joining("\n"));
    }

    private void executeAction(
        SupportTicket ticket,
        RouterResponse routerResponse
    ) throws BindException {
        log.info(
            "ToolExecution(route)({}) SupportTicket(id:{}) NextAction({})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            routerResponse.getNextAction()
        );

        runtimeActionAdapterService.execute(
            ticket,
            routerResponse
        );

        log.info(
            "ToolExecution(route)({}) SupportTicket(id:{}) NextAction({}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId(),
            routerResponse.getNextAction()
        );
    }

    private RouterResponse baseResponse(
        SupportTicket ticket,
        NextAction nextAction
    ) {
        return RouterResponse.builder()
                             .category(Objects.requireNonNullElse(ticket.getCurrentCategory(), TicketCategory.OTHER))
                             .priority(Objects.requireNonNullElse(ticket.getCurrentPriority(), TicketPriority.MEDIUM))
                             .queue(Objects.requireNonNullElse(ticket.getAssignedQueue(), TicketQueue.GENERAL_Q))
                             .nextAction(nextAction)
                             .confidence(Objects.requireNonNullElse(ticket.getLatestRoutingConfidence(), BigDecimal.ONE))
                             .build();
    }

    private String formatKnowledgeDocument(
        Document document
    ) {
        Object articleId = document.getMetadata().get(
            VectorStoreMetadataKey.ARTICLE_ID.name()
        );
        String title = StringUtils.trimToEmpty(
            StringUtils.substringBefore(
                StringUtils.defaultString(document.getText()),
                System.lineSeparator()
            )
        );

        return String.format(
            "article_id=%s | title=%s",
            Objects.toString(articleId, "n/a"),
            StringUtils.abbreviate(title, 100)
        );
    }

    private void putIfNotBlank(
        Map<String, Object> target,
        String key,
        String value
    ) {
        if (StringUtils.isNotBlank(value)) {
            target.put(
                key,
                value
            );
        }
    }

    private int normalizeLimit(
        Integer limit,
        int fallback,
        int max
    ) {
        if (Objects.isNull(limit) || limit <= 0) {
            return fallback;
        }

        return Math.min(
            limit,
            max
        );
    }

    private SupportTicket currentTicket() {
        return ToolExecutionContext.currentTicket();
    }
}
