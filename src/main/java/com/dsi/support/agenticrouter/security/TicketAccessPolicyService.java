package com.dsi.support.agenticrouter.security;

import com.dsi.support.agenticrouter.entity.AgentQueueMembership;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketQueryScope;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.AgentQueueMembershipRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TicketAccessPolicyService {

    private final AgentQueueMembershipRepository agentQueueMembershipRepository;
    private final SupportTicketRepository supportTicketRepository;

    public boolean canAccessTicket(
        Long ticketId,
        AppUser actor
    ) {
        if (Objects.isNull(ticketId) || Objects.isNull(actor)) {
            return false;
        }

        Optional<SupportTicket> supportTicket = supportTicketRepository.findById(ticketId);
        return supportTicket.filter(ticket -> canAccessTicket(ticket, actor)).isPresent();
    }

    public boolean canAccessTicket(
        SupportTicket supportTicket,
        AppUser actor
    ) {
        if (Objects.isNull(supportTicket) || Objects.isNull(actor)) {
            return false;
        }

        if (actor.isAdmin() || actor.isSupervisor()) {
            return true;
        }

        if (actor.isCustomer()) {
            return Objects.equals(
                supportTicket.getCustomer().getId(),
                actor.getId()
            );
        }

        if (actor.isAgent()) {
            if (Objects.nonNull(supportTicket.getAssignedAgent())
                && Objects.equals(
                supportTicket.getAssignedAgent().getId(),
                actor.getId()
            )) {
                return true;
            }

            if (Objects.isNull(supportTicket.getAssignedQueue())) {
                return false;
            }

            return hasQueueAccess(
                actor,
                supportTicket.getAssignedQueue()
            );
        }

        return false;
    }

    public boolean canAccessQueueScope(
        TicketQueryScope scope,
        TicketQueue queue,
        AppUser actor
    ) {
        if (Objects.isNull(scope) || Objects.isNull(actor)) {
            return false;
        }

        return switch (scope) {
            case MINE -> true;
            case REVIEW, ALL -> actor.isSupervisor() || actor.isAdmin();
            case QUEUE -> Objects.isNull(queue)
                ? actor.isSupervisor() || actor.isAdmin()
                : canAccessQueue(queue, actor);
        };
    }

    public boolean canAccessQueue(
        TicketQueue queue,
        AppUser actor
    ) {
        if (Objects.isNull(queue) || Objects.isNull(actor)) {
            return false;
        }

        if (actor.isSupervisor() || actor.isAdmin()) {
            return true;
        }

        if (actor.isAgent()) {
            return hasQueueAccess(actor, queue);
        }

        return false;
    }

    public Set<TicketStatus> allowedStatusTransitions(
        SupportTicket supportTicket,
        AppUser actor
    ) {
        if (Objects.isNull(supportTicket) || Objects.isNull(actor)) {
            return Collections.emptySet();
        }

        if (actor.isSupervisor() || actor.isAdmin()) {
            return Collections.emptySet();
        }

        if (!actor.isAgent()) {
            return Collections.emptySet();
        }

        if (Objects.isNull(supportTicket.getAssignedAgent())
            || !Objects.equals(supportTicket.getAssignedAgent().getId(), actor.getId())) {
            return Collections.emptySet();
        }

        return switch (supportTicket.getStatus()) {
            case ASSIGNED -> EnumSet.of(TicketStatus.IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(
                TicketStatus.WAITING_CUSTOMER,
                TicketStatus.RESOLVED,
                TicketStatus.ESCALATED
            );
            case WAITING_CUSTOMER -> EnumSet.of(
                TicketStatus.IN_PROGRESS,
                TicketStatus.ESCALATED
            );
            case ESCALATED -> EnumSet.of(
                TicketStatus.IN_PROGRESS,
                TicketStatus.WAITING_CUSTOMER,
                TicketStatus.RESOLVED
            );
            default -> Collections.emptySet();
        };
    }

    public boolean canReply(
        SupportTicket supportTicket,
        AppUser actor
    ) {
        if (Objects.isNull(supportTicket) || Objects.isNull(actor)) {
            return false;
        }

        if (actor.isSupervisor() || actor.isAdmin()) {
            return false;
        }

        if (actor.isAgent()) {
            return Objects.nonNull(supportTicket.getAssignedAgent())
                   && Objects.equals(supportTicket.getAssignedAgent().getId(), actor.getId());
        }

        return actor.isCustomer() && Objects.equals(supportTicket.getCustomer().getId(), actor.getId());
    }

    public boolean canAssignSelf(
        SupportTicket supportTicket,
        AppUser actor
    ) {
        if (Objects.isNull(supportTicket) || Objects.isNull(actor)) {
            return false;
        }

        if (!actor.isAgent()) {
            return false;
        }

        if (Objects.nonNull(supportTicket.getAssignedAgent())) {
            return false;
        }

        if (supportTicket.isRequiresHumanReview()) {
            return false;
        }

        return Objects.nonNull(supportTicket.getAssignedQueue())
               && hasQueueAccess(actor, supportTicket.getAssignedQueue());
    }

    public boolean canAssignOthers(
        AppUser actor
    ) {
        return Objects.nonNull(actor) && (actor.isSupervisor() || actor.isAdmin());
    }

    public boolean canOverrideRouting(
        AppUser actor
    ) {
        return Objects.nonNull(actor) && (actor.isSupervisor() || actor.isAdmin());
    }

    public boolean canResolveEscalation(
        AppUser actor
    ) {
        return Objects.nonNull(actor) && (actor.isSupervisor() || actor.isAdmin());
    }

    public List<TicketQueue> accessibleQueues(
        AppUser actor
    ) {
        if (Objects.isNull(actor)) {
            return List.of();
        }

        if (actor.isSupervisor() || actor.isAdmin()) {
            return List.of(TicketQueue.values());
        }

        if (!actor.isAgent() || Objects.isNull(actor.getId())) {
            return List.of();
        }

        return agentQueueMembershipRepository.findByUserId(actor.getId())
                                             .stream()
                                             .map(AgentQueueMembership::getQueue)
                                             .distinct()
                                             .sorted()
                                             .toList();
    }

    private boolean hasQueueAccess(
        AppUser actor,
        TicketQueue queue
    ) {
        if (actor.isSupervisor() || actor.isAdmin()) {
            return true;
        }

        if (!actor.isAgent() || Objects.isNull(actor.getId())) {
            return false;
        }

        return agentQueueMembershipRepository.existsByUserIdAndQueue(
            actor.getId(),
            queue
        );
    }
}
