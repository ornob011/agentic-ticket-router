package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.Escalation;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.EscalationFilterStatus;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.EscalationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketEscalationService {

    private final EscalationRepository escalationRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AppUserRepository appUserRepository;
    private final TicketAccessPolicyService ticketAccessPolicyService;
    private final AuditService auditService;

    public void resolveEscalation(
        Long escalationId,
        String resolutionNotes
    ) throws BindException {
        log.info(
            "EscalationResolve({}) Escalation(id:{}) Actor(id:{}) Outcome(notesLength:{})",
            OperationalLogContext.PHASE_START,
            escalationId,
            Utils.getLoggedInUserId(),
            StringUtils.length(StringUtils.trimToNull(resolutionNotes))
        );

        Objects.requireNonNull(escalationId, "escalationId");
        Objects.requireNonNull(resolutionNotes, "resolutionNotes");
        if (!StringUtils.isNotBlank(resolutionNotes)) {
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

        AppUser resolver = appUserRepository.findById(Utils.getLoggedInUserId())
                                            .orElseThrow(
                                                DataNotFoundException.supplier(
                                                    AppUser.class,
                                                    Utils.getLoggedInUserId()
                                                )
                                            );
        if (!ticketAccessPolicyService.canResolveEscalation(resolver)) {
            throw BindValidation.fieldError(
                "resolveEscalationRequest",
                "escalationId",
                "Actor cannot resolve escalation"
            );
        }

        escalation.markResolved(
            resolver,
            resolutionNotes
        );

        escalationRepository.save(escalation);

        SupportTicket supportTicket = escalation.getTicket();
        supportTicket.setStatus(TicketStatus.IN_PROGRESS);
        supportTicket.setEscalated(false);
        supportTicket.setRequiresHumanReview(false);
        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.ESCALATION_RESOLVED,
            escalation.getTicket().getId(),
            Utils.getLoggedInUserId(),
            String.format("Escalation resolved: %s", resolutionNotes),
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
        Pageable pageable
    ) {
        EscalationFilterStatus effectiveFilterStatus = Objects.requireNonNullElse(
            escalationFilterStatus,
            EscalationFilterStatus.ALL
        );

        Page<Escalation> escalationPage = switch (effectiveFilterStatus) {
            case RESOLVED -> escalationRepository.findByResolvedTrue(pageable);
            case PENDING -> escalationRepository.findByResolvedFalse(pageable);
            case ALL -> escalationRepository.findAll(pageable);
        };

        List<ApiDtos.EscalationSummary> content = escalationPage.map(this::toEscalationSummaryDto)
                                                                .getContent();

        return ApiDtos.PagedResponse.<ApiDtos.EscalationSummary>builder()
                                    .content(content)
                                    .page(escalationPage.getNumber())
                                    .size(escalationPage.getSize())
                                    .totalElements(escalationPage.getTotalElements())
                                    .totalPages(escalationPage.getTotalPages())
                                    .hasNext(escalationPage.hasNext())
                                    .build();
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
