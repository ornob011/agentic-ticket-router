package com.dsi.support.agenticrouter.service.memory;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.MemoryScope;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemoryContextService {

    private static final String CUSTOMER_MEMORY_TICKET_PREFIX = "[Ticket %s] %s";
    private static final int MAX_CACHE_SIZE = 1000;

    private final ChatMemory ticketHistoryChatMemory;
    private final ChatMemory customerContextChatMemory;
    private final ChatMemory agentPatternChatMemory;
    private final ConcurrentHashMap<String, Integer> recentMessageHashes = new ConcurrentHashMap<>();

    public void appendCustomerMessage(
        SupportTicket ticket,
        String customerMessage
    ) {
        log.debug(
            "MemoryContextAppendCustomer({}) SupportTicket(id:{},ticketNo:{}) Customer(id:{})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            ticket.getTicketNo(),
            ticket.getCustomer().getId()
        );

        MemoryContext memoryContext = createMemoryContext(
            ticket
        );

        addUserMessage(
            ticketHistoryChatMemory,
            memoryContext.ticketConversationId(),
            customerMessage
        );

        addUserMessage(
            customerContextChatMemory,
            memoryContext.customerConversationId(),
            formatCustomerMemoryMessage(
                ticket,
                customerMessage
            )
        );
    }

    public void appendAssistantMessage(
        SupportTicket ticket,
        String assistantMessage
    ) {
        log.debug(
            "MemoryContextAppendAssistant({}) SupportTicket(id:{},ticketNo:{}) Customer(id:{})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            ticket.getTicketNo(),
            ticket.getCustomer().getId()
        );

        MemoryContext memoryContext = createMemoryContext(
            ticket
        );

        addAssistantMessage(
            ticketHistoryChatMemory,
            memoryContext.ticketConversationId(),
            assistantMessage
        );

        addAssistantMessage(
            customerContextChatMemory,
            memoryContext.customerConversationId(),
            formatCustomerMemoryMessage(
                ticket,
                assistantMessage
            )
        );
    }

    public void registerConversation(
        SupportTicket ticket,
        String userMessage,
        String assistantMessage
    ) {
        log.debug(
            "MemoryContextRegister({}) SupportTicket(id:{},ticketNo:{}) Customer(id:{}) Outcome(assistantIncluded:{})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            ticket.getTicketNo(),
            ticket.getCustomer().getId(),
            Objects.nonNull(assistantMessage)
        );

        MemoryContext memoryContext = createMemoryContext(
            ticket
        );

        addUserMessage(
            ticketHistoryChatMemory,
            memoryContext.ticketConversationId(),
            userMessage
        );

        addUserMessage(
            customerContextChatMemory,
            memoryContext.customerConversationId(),
            formatCustomerMemoryMessage(
                ticket,
                userMessage
            )
        );

        if (Objects.nonNull(assistantMessage)) {
            addAssistantMessage(
                ticketHistoryChatMemory,
                memoryContext.ticketConversationId(),
                assistantMessage
            );
            addAssistantMessage(
                customerContextChatMemory,
                memoryContext.customerConversationId(),
                formatCustomerMemoryMessage(
                    ticket,
                    assistantMessage
                )
            );
        }

        log.debug(
            "MemoryContextRegister({}) SupportTicket(id:{}) Outcome(ticketConversationId:{},customerConversationId:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId(),
            memoryContext.ticketConversationId(),
            memoryContext.customerConversationId()
        );
    }

    public List<Message> getTicketHistory(
        Long ticketId
    ) {
        String conversationId = MemoryScope.TICKET.formatConversationId(
            ticketId
        );

        return ticketHistoryChatMemory.get(
            conversationId
        );
    }

    public List<Message> getCustomerContext(
        Long customerId
    ) {
        String conversationId = MemoryScope.CUSTOMER.formatConversationId(
            customerId
        );

        return customerContextChatMemory.get(
            conversationId
        );
    }

    public List<Message> getAgentPatterns(
        Long agentId
    ) {
        String conversationId = MemoryScope.AGENT.formatConversationId(
            agentId
        );

        return agentPatternChatMemory.get(
            conversationId
        );
    }

    public void clearTicketHistory(
        Long ticketId
    ) {
        String conversationId = MemoryScope.TICKET.formatConversationId(
            ticketId
        );

        ticketHistoryChatMemory.clear(
            conversationId
        );

        log.debug(
            "MemoryContextClear({}) ticketId:{}",
            OperationalLogContext.PHASE_PERSIST,
            ticketId
        );
    }

    public void clearCustomerContext(
        Long customerId
    ) {
        String conversationId = MemoryScope.CUSTOMER.formatConversationId(
            customerId
        );

        customerContextChatMemory.clear(
            conversationId
        );

        log.debug(
            "MemoryContextClear({}) customerId:{}",
            OperationalLogContext.PHASE_PERSIST,
            customerId
        );
    }

    public MemoryContext createMemoryContext(
        SupportTicket ticket
    ) {
        String ticketConversationId = MemoryScope.TICKET.formatConversationId(
            ticket.getId()
        );

        String customerConversationId = MemoryScope.CUSTOMER.formatConversationId(
            ticket.getCustomer().getId()
        );

        String agentConversationId = resolveAgentConversationId(
            ticket
        );

        return new MemoryContext(
            ticketConversationId,
            customerConversationId,
            agentConversationId
        );
    }

    private String resolveAgentConversationId(
        SupportTicket ticket
    ) {
        if (Objects.isNull(ticket.getAssignedAgent())) {
            return null;
        }

        return MemoryScope.AGENT.formatConversationId(
            ticket.getAssignedAgent().getId()
        );
    }

    private void addUserMessage(
        ChatMemory chatMemory,
        String conversationId,
        String userMessage
    ) {
        if (shouldSkipAppend(
            chatMemory,
            conversationId,
            MessageType.USER,
            userMessage
        )) {
            return;
        }

        chatMemory.add(
            conversationId,
            new UserMessage(
                userMessage
            )
        );
    }

    private void addAssistantMessage(
        ChatMemory chatMemory,
        String conversationId,
        String assistantMessage
    ) {
        if (shouldSkipAppend(
            chatMemory,
            conversationId,
            MessageType.ASSISTANT,
            assistantMessage
        )) {
            return;
        }

        chatMemory.add(
            conversationId,
            new AssistantMessage(
                assistantMessage
            )
        );
    }

    private boolean shouldSkipAppend(
        ChatMemory chatMemory,
        String conversationId,
        MessageType messageType,
        String content
    ) {
        String cacheKey = conversationId + ":" + messageType.name();
        int contentHash = StringUtils.isNotBlank(content) ? content.hashCode() : 0;

        Integer lastHash = recentMessageHashes.get(cacheKey);
        if (Objects.nonNull(lastHash) && lastHash == contentHash) {
            log.debug(
                "MemoryContextAppend({}) conversationId:{} messageType:{} Outcome(skipped:{},source:{})",
                OperationalLogContext.PHASE_SKIP,
                conversationId,
                messageType,
                true,
                "local_cache"
            );
            return true;
        }

        List<Message> existingMessages = chatMemory.get(
            conversationId
        );

        if (existingMessages.isEmpty()) {
            recentMessageHashes.put(cacheKey, contentHash);
            trimCacheIfNeeded();
            return false;
        }

        Message lastMessage = existingMessages.getLast(
        );

        boolean shouldSkip = lastMessage.getMessageType() == messageType
                             && StringUtils.normalizeSpace(lastMessage.getText())
                                           .equals(
                                               StringUtils.normalizeSpace(content)
                                           );

        if (shouldSkip) {
            log.debug(
                "MemoryContextAppend({}) conversationId:{} messageType:{} Outcome(skipped:{},source:{})",
                OperationalLogContext.PHASE_SKIP,
                conversationId,
                messageType,
                true,
                "db_check"
            );
        } else {
            recentMessageHashes.put(cacheKey, contentHash);
            trimCacheIfNeeded();
        }

        return shouldSkip;
    }

    private void trimCacheIfNeeded() {
        if (recentMessageHashes.size() > MAX_CACHE_SIZE) {
            recentMessageHashes.clear();
        }
    }

    private String formatCustomerMemoryMessage(
        SupportTicket ticket,
        String content
    ) {
        return String.format(
            CUSTOMER_MEMORY_TICKET_PREFIX,
            ticket.getTicketNo(),
            content
        );
    }

    public record MemoryContext(
        String ticketConversationId,
        String customerConversationId,
        String agentConversationId
    ) {
        public boolean hasAgentContext() {
            return Objects.nonNull(
                agentConversationId
            );
        }
    }
}
