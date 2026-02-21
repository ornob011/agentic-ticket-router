package com.dsi.support.agenticrouter.service.agentruntime.streaming;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.ai.LlmPromptCaller;
import com.dsi.support.agenticrouter.service.ai.PromptService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingDraftService {

    private final ChatModel chatModel;
    private final LlmPromptCaller llmPromptCaller;
    private final PromptService promptService;
    private final TicketMessageRepository ticketMessageRepository;

    public Flux<String> streamDraftReply(
        SupportTicket supportTicket
    ) {
        log.info(
            "StreamingDraft({}) SupportTicket(id:{}) Outcome(start)",
            OperationalLogContext.PHASE_START,
            supportTicket.getId()
        );

        String conversationHistory = buildConversationHistory(supportTicket);

        return llmPromptCaller.streamContent(
            chatModel,
            promptUserSpec -> promptUserSpec
                .text(promptService.getTicketReplyDraftPrompt())
                .param("subject", supportTicket.getSubject())
                .param("conversation_history", conversationHistory)
        ).doOnComplete(() -> log.info(
            "StreamingDraft({}) SupportTicket(id:{}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId()
        )).doOnError(error -> log.error(
            "StreamingDraft({}) SupportTicket(id:{}) Outcome(reason:{})",
            OperationalLogContext.PHASE_FAIL,
            supportTicket.getId(),
            "stream_failed",
            error
        ));
    }

    private String buildConversationHistory(
        SupportTicket supportTicket
    ) {
        String history = ticketMessageRepository.findByTicketIdWithAuthorOrderByCreatedAtAsc(supportTicket.getId())
                                                .stream()
                                                .map(message -> String.format(
                                                    "[%s] %s: %s",
                                                    message.getCreatedAt(),
                                                    message.getMessageKind(),
                                                    message.getContent()
                                                ))
                                                .collect(Collectors.joining("\n"));

        log.debug(
            "StreamingDraft({}) SupportTicket(id:{}) Outcome(historyLength:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            StringUtils.length(history)
        );

        return history;
    }
}
