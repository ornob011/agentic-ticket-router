package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.KnowledgeArticle;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.KnowledgeArticleRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.KnowledgeBaseService;
import com.dsi.support.agenticrouter.service.NotificationService;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UseKnowledgeArticleAction implements TicketAction {

    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.USE_KNOWLEDGE_ARTICLE.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse response
    ) {
        log.info(
            "UseKnowledgeArticleAction({}) SupportTicket(id:{},status:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus()
        );

        ActionParams actionParams = new ActionParams(response);

        Long articleId = actionParams.articleId();

        KnowledgeArticle article = knowledgeArticleRepository.findById(articleId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     KnowledgeArticle.class,
                                                                     articleId
                                                                 )
                                                             );

        log.info(
            "UseKnowledgeArticleAction({}) SupportTicket(id:{}) KnowledgeArticle(id:{},category:{},priority:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            article.getId(),
            article.getCategory(),
            article.getPriority()
        );

        TicketMessage ticketMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .messageKind(MessageKind.AUTO_REPLY)
                                                   .content(article.getContent())
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(ticketMessage);

        resolveTicket(
            supportTicket
        );

        supportTicketRepository.save(supportTicket);

        log.info(
            "UseKnowledgeArticleAction({}) SupportTicket(id:{},status:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus()
        );

        knowledgeBaseService.recordUsage(
            articleId,
            true
        );

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            NotificationType.STATUS_CHANGE,
            Messages.ticketResolvedTitle(supportTicket),
            Messages.ticketResolvedBody(),
            supportTicket.getId()
        );

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            null,
            "Knowledge base article sent: " + article.getTitle(),
            AuditMetadata.articleId(articleId)
        );

        log.info(
            "UseKnowledgeArticleAction({}) SupportTicket(id:{},status:{}) KnowledgeArticle(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            articleId
        );
    }

    private void resolveTicket(
        SupportTicket supportTicket
    ) {
        supportTicket.setStatus(TicketStatus.RESOLVED);
        supportTicket.setResolvedAt(Instant.now());
        supportTicket.updateLastActivity();
    }

    private enum ActionParamKey {
        ARTICLE_ID("article_id");

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

        public Long articleId() {
            String rawArticleId = text(ActionParamKey.ARTICLE_ID)
                .orElseThrow(() -> new IllegalStateException(
                    ActionParamKey.ARTICLE_ID.key() + " is required"
                ));

            if (!StringUtils.isNumeric(rawArticleId)) {
                throw new IllegalStateException(ActionParamKey.ARTICLE_ID.key() + " must be numeric");
            }

            return Long.parseLong(rawArticleId);
        }

        private Optional<String> text(
            ActionParamKey key
        ) {
            return Optional.ofNullable(values.get(key.key()))
                           .map(v -> Objects.toString(v, null))
                           .map(StringUtils::trimToNull);
        }
    }

    public static final class AuditMetadata {

        private static final String KEY_ARTICLE_ID = "article_id";

        private AuditMetadata() {
        }

        public static JsonNode articleId(
            Long articleId
        ) {
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.put(KEY_ARTICLE_ID, articleId);
            return node;
        }
    }

    private static final class Messages {

        private Messages() {
        }

        public static String ticketResolvedTitle(
            SupportTicket supportTicket
        ) {
            return "Ticket Resolved: " + supportTicket.getFormattedTicketNo();
        }

        public static String ticketResolvedBody() {
            return "Your ticket has been automatically resolved using a knowledge base article. " +
                   "If you need further assistance, please reply.";
        }
    }
}
