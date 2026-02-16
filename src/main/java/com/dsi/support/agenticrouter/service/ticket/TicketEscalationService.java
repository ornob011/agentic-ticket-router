package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.Escalation;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.EscalationFilterStatus;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.EscalationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketEscalationService {
    private static final Sort DEFAULT_ESCALATION_SORT = Sort.by("createdAt").descending();

    private final EscalationRepository escalationRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final TicketAccessPolicyService ticketAccessPolicyService;
    private final AuditService auditService;
    private final TicketCommandLookupService ticketCommandLookupService;
    private final TicketWorkflowUpdateService ticketWorkflowUpdateService;

    public void resolveEscalation(
        Long escalationId,
        String resolutionNotes
    ) throws BindException {
        log.info(
            "EscalationResolve({}) Escalation(id:{}) Actor(id:{}) Outcome(notesLength:{})",
            OperationalLogContext.PHASE_START,
            escalationId,
            Utils.getLoggedInUserId(),
            StringUtils.length(StringNormalizationUtils.trimToNull(resolutionNotes))
        );

        Objects.requireNonNull(escalationId, "escalationId");
        String normalizedResolutionNotes = StringNormalizationUtils.trimToNull(resolutionNotes);
        if (Objects.isNull(normalizedResolutionNotes)) {
            throw BindValidation.fieldError(
                "resolveEscalationRequest",
                "resolutionNotes",
                "Resolution notes are required."
            );
        }

        Escalation escalation = escalationRepository.findById(escalationId)
                                                    .orElseThrow(
                                                        DataNotFoundException.supplier(
                                                            Escalation.class,
                                                            escalationId
                                                        )
                                                    );

        if (escalation.isResolved()) {
            log.warn(
                "EscalationResolve({}) Escalation(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                escalationId,
                "already_resolved"
            );

            throw BindValidation.fieldError(
                "resolveEscalationRequest",
                "escalationId",
                "Escalation already resolved: " + escalationId
            );
        }

        AppUser resolver = ticketCommandLookupService.requireCurrentActor();
        if (!ticketAccessPolicyService.canResolveEscalation(resolver)) {
            throw BindValidation.fieldError(
                "resolveEscalationRequest",
                "escalationId",
                "Actor cannot resolve escalation"
            );
        }

        escalation.markResolved(
            resolver,
            normalizedResolutionNotes
        );

        escalationRepository.save(escalation);

        SupportTicket supportTicket = escalation.getTicket();
        supportTicket.setStatus(TicketStatus.IN_PROGRESS);
        supportTicket.setEscalated(false);
        ticketWorkflowUpdateService.completeHumanReviewIfSupervisorDecision(
            supportTicket,
            resolver
        );
        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.ESCALATION_RESOLVED,
            escalation.getTicket().getId(),
            resolver.getId(),
            String.format("Escalation resolved: %s", normalizedResolutionNotes),
            null
        );

        log.info(
            "EscalationResolve({}) Escalation(id:{}) SupportTicket(id:{},status:{}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            escalation.getId(),
            escalation.getTicket().getId(),
            escalation.getTicket().getStatus(),
            resolver.getId()
        );
    }

    @Transactional(readOnly = true)
    public ApiDtos.PagedResponse<ApiDtos.EscalationSummary> listEscalationSummaries(
        EscalationFilterStatus escalationFilterStatus,
        int page,
        int size
    ) {
        return listEscalationSummaries(
            escalationFilterStatus,
            PaginationUtils.normalize(
                page,
                size,
                DEFAULT_ESCALATION_SORT
            )
        );
    }

    @Transactional(readOnly = true)
    public ApiDtos.PagedResponse<ApiDtos.EscalationSummary> listEscalationSummaries(
        EscalationFilterStatus escalationFilterStatus,
        Pageable pageable
    ) {
        Pageable normalizedPageable = PaginationUtils.normalize(
            pageable,
            DEFAULT_ESCALATION_SORT
        );

        EscalationFilterStatus effectiveFilterStatus = Objects.requireNonNullElse(
            escalationFilterStatus,
            EscalationFilterStatus.ALL
        );

        Page<Escalation> escalationPage = switch (effectiveFilterStatus) {
            case RESOLVED -> escalationRepository.findByResolvedTrue(normalizedPageable);
            case PENDING -> escalationRepository.findByResolvedFalse(normalizedPageable);
            case ALL -> escalationRepository.findAll(normalizedPageable);
        };

        return PageResponseUtils.fromPage(
            escalationPage,
            this::toEscalationSummaryDto
        );
    }

    @Transactional(readOnly = true)
    public ApiDtos.EscalationDetail getEscalationDetail(
        Long escalationId
    ) {
        Escalation escalation = escalationRepository.findById(escalationId)
                                                    .orElseThrow(
                                                        DataNotFoundException.supplier(
                                                            Escalation.class,
                                                            escalationId
                                                        )
                                                    );

        return ApiDtos.EscalationDetail.builder()
                                       .id(escalation.getId())
                                       .ticketId(escalation.getTicket().getId())
                                       .formattedTicketNo(escalation.getTicket().getFormattedTicketNo())
                                       .reason(escalation.getReason())
                                       .resolved(escalation.isResolved())
                                       .resolutionNotes(escalation.getResolutionNotes())
                                       .createdAt(escalation.getCreatedAt())
                                       .resolvedAt(escalation.getResolvedAt())
                                       .assignedSupervisor(
                                           Objects.isNull(escalation.getAssignedSupervisor())
                                               ? null
                                               : escalation.getAssignedSupervisor().getFullName()
                                       )
                                       .resolvedBy(
                                           Objects.isNull(escalation.getResolvedBy())
                                               ? null
                                               : escalation.getResolvedBy().getFullName()
                                       )
                                       .build();
    }

    private ApiDtos.EscalationSummary toEscalationSummaryDto(Escalation escalation) {
        return ApiDtos.EscalationSummary.builder()
                                        .id(escalation.getId())
                                        .ticketId(escalation.getTicket().getId())
                                        .formattedTicketNo(escalation.getTicket().getFormattedTicketNo())
                                        .reason(escalation.getReason())
                                        .resolved(escalation.isResolved())
                                        .createdAt(escalation.getCreatedAt())
                                        .assignedSupervisor(
                                            Objects.isNull(escalation.getAssignedSupervisor())
                                                ? null
                                                : escalation.getAssignedSupervisor().getFullName()
                                        )
                                        .build();
    }

}
