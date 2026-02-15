package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.Escalation;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.EscalationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketStatusCommandService {

    private static final String BUSINESS_DRIVER_MANUAL_STATUS_CHANGE = "Manual Status Update";
    private static final String BUSINESS_DRIVER_UNSPECIFIED = "System";
    private static final BiConsumer<SupportTicket, Instant> NO_STATUS_SIDE_EFFECT = (supportTicket, statusChangeTimestamp) -> {
    };

    private static final Map<TicketStatus, BiConsumer<SupportTicket, Instant>> STATUS_SIDE_EFFECTS = new EnumMap<>(TicketStatus.class);

    static {
        STATUS_SIDE_EFFECTS.put(
            TicketStatus.RESOLVED,
            (supportTicket, statusChangeTimestamp) -> {
                if (supportTicket.getResolvedAt() == null) {
                    supportTicket.setResolvedAt(statusChangeTimestamp);
                }
            }
        );

        STATUS_SIDE_EFFECTS.put(
            TicketStatus.CLOSED,
            (supportTicket, statusChangeTimestamp) -> {
                if (supportTicket.getClosedAt() == null) {
                    supportTicket.setClosedAt(statusChangeTimestamp);
                }
            }
        );
    }

    private final SupportTicketRepository supportTicketRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AppUserRepository appUserRepository;
    private final EscalationRepository escalationRepository;
    private final TicketAccessPolicyService ticketAccessPolicyService;

    public void changeTicketStatus(
        Long ticketId,
        TicketStatus targetStatus,
        String reason
    ) throws BindException {
        changeTicketStatus(
            ticketId,
            targetStatus,
            BUSINESS_DRIVER_MANUAL_STATUS_CHANGE,
            reason
        );
    }

    public void changeTicketStatus(
        Long ticketId,
        TicketStatus targetStatus,
        String businessDriver,
        String reason
    ) throws BindException {
        log.info(
            "TicketStatusChange({}) SupportTicket(id:{}) Outcome(targetStatus:{},businessDriver:{},reasonLength:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            targetStatus,
            businessDriver,
            StringUtils.length(StringUtils.trimToNull(reason))
        );

        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(targetStatus, "targetStatus");

        Long actorId = Utils.getLoggedInUserId();

        AppUser actor = appUserRepository.findById(actorId)
                                         .orElseThrow(
                                             DataNotFoundException.supplier(
                                                 AppUser.class,
                                                 actorId
                                             )
                                         );

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        TicketStatus previousStatus = supportTicket.getStatus();
        if (previousStatus == targetStatus) {
            log.debug(
                "TicketStatusChange({}) SupportTicket(id:{},status:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                supportTicket.getId(),
                supportTicket.getStatus(),
                "no_status_change"
            );

            return;
        }

        Set<TicketStatus> allowedTransitions = ticketAccessPolicyService.allowedStatusTransitions(
            supportTicket,
            actor
        );
        if (!allowedTransitions.contains(targetStatus)) {
            throw new IllegalStateException(
                String.format(
                    "Transition %s -> %s is not allowed for actor role %s",
                    previousStatus,
                    targetStatus,
                    actor.getRole()
                )
            );
        }

        if (previousStatus == TicketStatus.ESCALATED) {
            boolean hasOpenEscalation = escalationRepository.findByTicketId(supportTicket.getId())
                                                            .map(existingEscalation -> !existingEscalation.isResolved())
                                                            .orElse(false);
            if (hasOpenEscalation) {
                throwBindValidation(
                    "ticketStatusRequest",
                    "newStatus",
                    "Please resolve the escalation before changing ticket status."
                );
            }
        }

        Instant statusChangeTimestamp = Instant.now();

        supportTicket.setStatus(targetStatus);
        completeHumanReviewIfSupervisorDecision(
            supportTicket,
            actor
        );
        supportTicket.updateLastActivity();

        STATUS_SIDE_EFFECTS.getOrDefault(targetStatus, NO_STATUS_SIDE_EFFECT)
                           .accept(supportTicket, statusChangeTimestamp);

        String normalizedBusinessDriver = Optional.ofNullable(businessDriver)
                                                  .map(String::trim)
                                                  .filter(StringUtils::isNotBlank)
                                                  .orElse(BUSINESS_DRIVER_UNSPECIFIED);

        String normalizedReason = Optional.ofNullable(reason)
                                          .map(String::trim)
                                          .filter(StringUtils::isNotBlank)
                                          .orElse("No reason provided");

        if (targetStatus == TicketStatus.ESCALATED && !StringUtils.isNotBlank(reason)) {
            throwBindValidation(
                "ticketStatusRequest",
                "reason",
                "Escalation reason is required."
            );
        }

        auditService.recordEvent(
            AuditEventType.TICKET_STATUS_CHANGED,
            supportTicket.getId(),
            actor.getId(),
            String.format(
                "Status changed from %s to %s. Triggered by: %s. Reason: %s.",
                previousStatus, targetStatus, normalizedBusinessDriver, normalizedReason
            ),
            null
        );

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            NotificationType.STATUS_CHANGE,
            String.format("Status Updated: %s", supportTicket.getFormattedTicketNo()),
            String.format("Your ticket status is now: %s", targetStatus),
            supportTicket.getId()
        );

        if (targetStatus == TicketStatus.ESCALATED) {
            String escalationReason = String.format(
                "Manual escalation by %s. Triggered by: %s. Reason: %s.",
                actor.getRole(),
                normalizedBusinessDriver,
                normalizedReason
            );

            escalationRepository.findByTicketId(supportTicket.getId())
                                .ifPresentOrElse(
                                    escalation -> escalation.reopen(escalationReason),
                                    () -> escalationRepository.save(
                                        Escalation.builder()
                                                  .ticket(supportTicket)
                                                  .reason(escalationReason)
                                                  .resolved(false)
                                                  .build()
                                    )
                                );
            supportTicket.setEscalated(true);
        } else {
            supportTicket.setEscalated(false);
        }

        supportTicketRepository.save(supportTicket);

        log.info(
            "TicketStatusChange({}) SupportTicket(id:{},fromStatus:{},toStatus:{}) Actor(id:{},role:{}) Outcome(driver:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            previousStatus,
            supportTicket.getStatus(),
            actor.getId(),
            actor.getRole(),
            normalizedBusinessDriver
        );
    }

    private void completeHumanReviewIfSupervisorDecision(
        SupportTicket supportTicket,
        AppUser actor
    ) {
        if (Objects.nonNull(actor) && (actor.isSupervisor() || actor.isAdmin())) {
            supportTicket.setRequiresHumanReview(false);
        }
    }

    private void throwBindValidation(
        String objectName,
        String fieldName,
        String message
    ) throws BindException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
            new Object(),
            objectName
        );

        bindingResult.rejectValue(
            fieldName,
            "validation.error",
            message
        );

        throw new BindException(bindingResult);
    }
}
