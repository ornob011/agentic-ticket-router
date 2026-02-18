package com.dsi.support.agenticrouter.service.action.handlers;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.Escalation;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.EscalationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class EscalateAction implements TicketAction {

    private final EscalationRepository escalationRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.ESCALATE.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        log.info(
            "EscalateAction({}) SupportTicket(id:{},status:{}) RouterResponse(queue:{},category:{},confidence:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus(),
            routerResponse.getQueue(),
            routerResponse.getCategory(),
            routerResponse.getConfidence()
        );

        String reason = StringUtils.defaultIfBlank(
            routerResponse.getInternalNote(),
            buildFallbackReason(
                routerResponse
            )
        );

        Escalation escalation = escalationRepository.findByTicketId(supportTicket.getId())
                                                    .map(existingEscalation -> {
                                                        existingEscalation.reopen(reason);
                                                        return existingEscalation;
                                                    })
                                                    .orElseGet(() -> Escalation.builder()
                                                                               .ticket(supportTicket)
                                                                               .reason(reason)
                                                                               .resolved(false)
                                                                               .build());

        escalation = escalationRepository.save(escalation);

        supportTicket.setStatus(TicketStatus.ESCALATED);
        supportTicket.setEscalated(true);
        supportTicket.setRequiresHumanReview(false);
        supportTicket.setAssignedQueue(routerResponse.getQueue());
        supportTicket.updateLastActivity();

        supportTicketRepository.save(supportTicket);

        log.warn(
            "EscalateAction({}) SupportTicket(id:{},status:{},queue:{}) Escalation(id:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            escalation.getId()
        );

        auditService.recordEvent(
            AuditEventType.ESCALATION_CREATED,
            supportTicket.getId(),
            null,
            "Ticket escalated: " + reason,
            null
        );

        log.warn(
            "EscalateAction({}) SupportTicket(id:{},status:{},queue:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue()
        );
    }

    private String buildFallbackReason(
        RouterResponse routerResponse
    ) {
        String categoryLabel = StringUtils.defaultIfBlank(
            EnumDisplayNameResolver.resolve(routerResponse.getCategory()),
            "Unknown Category"
        );

        String queueLabel = StringUtils.defaultIfBlank(
            EnumDisplayNameResolver.resolve(routerResponse.getQueue()),
            "Unknown Queue"
        );

        String signals = formatRationaleTags(
            routerResponse.getRationaleTags()
        );

        return String.format(
            "Auto-escalated to %s under %s category. Signals: %s.",
            queueLabel,
            categoryLabel,
            signals
        );
    }

    private String formatRationaleTags(
        List<String> rationaleTags
    ) {
        if (CollectionUtils.isEmpty(rationaleTags)) {
            return "None";
        }

        String formattedTags = rationaleTags.stream()
                                            .map(this::humanizeTag)
                                            .filter(StringUtils::isNotBlank)
                                            .distinct()
                                            .collect(Collectors.joining(", "));

        return StringUtils.defaultIfBlank(
            formattedTags,
            "None"
        );
    }

    private String humanizeTag(
        String rationaleTag
    ) {
        String normalizedTag = StringUtils.defaultString(rationaleTag)
                                          .replace('_', ' ')
                                          .replace('-', ' ');

        normalizedTag = StringUtils.normalizeSpace(normalizedTag);
        normalizedTag = StringUtils.lowerCase(normalizedTag);

        return StringUtils.capitalize(
            normalizedTag
        );
    }
}
