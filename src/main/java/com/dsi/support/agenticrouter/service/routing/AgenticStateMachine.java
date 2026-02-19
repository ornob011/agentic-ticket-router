package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketRouting;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.model.TicketAutonomousMetadata;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.action.ActionRegistry;
import com.dsi.support.agenticrouter.service.ticket.AutonomousProgressService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AgenticStateMachine {

    private final ActionRegistry actionRegistry;
    private final AutonomousProgressService autonomousProgressService;
    private final SupportTicketRepository supportTicketRepository;

    public void executeAction(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) throws BindException {
        log.info(
            "StateMachineExecute({}) SupportTicket(id:{},status:{},queue:{},priority:{}) RouterResponse(nextAction:{},queue:{},priority:{},confidence:{})",
            OperationalLogContext.PHASE_START,
            OperationalLogContext.ticketId(supportTicket),
            OperationalLogContext.status(supportTicket),
            OperationalLogContext.queue(supportTicket),
            OperationalLogContext.priority(supportTicket),
            routerResponse.getNextAction(),
            routerResponse.getQueue(),
            routerResponse.getPriority(),
            routerResponse.getConfidence()
        );

        if (autonomousProgressService.shouldEscalate(supportTicket)) {
            forceEscalation(
                supportTicket,
                routerResponse
            );

            log.warn(
                "StateMachineExecute({}) SupportTicket(id:{},status:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_DECISION,
                OperationalLogContext.ticketId(supportTicket),
                OperationalLogContext.status(supportTicket),
                "autonomous_escalation_forced"
            );

            return;
        }

        applyRoutingDecisions(
            supportTicket,
            routerResponse
        );

        actionRegistry.execute(
            supportTicket,
            routerResponse
        );

        log.info(
            "StateMachineExecute({}) SupportTicket(id:{},status:{},queue:{},priority:{}) Outcome(nextAction:{})",
            OperationalLogContext.PHASE_COMPLETE,
            OperationalLogContext.ticketId(supportTicket),
            OperationalLogContext.status(supportTicket),
            OperationalLogContext.queue(supportTicket),
            OperationalLogContext.priority(supportTicket),
            routerResponse.getNextAction()
        );
    }

    private void applyRoutingDecisions(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        if (Objects.nonNull(routerResponse.getCategory())) {
            supportTicket.setCurrentCategory(
                routerResponse.getCategory()
            );
        }

        if (Objects.nonNull(routerResponse.getPriority())) {
            supportTicket.setCurrentPriority(
                routerResponse.getPriority()
            );
        }

        if (Objects.nonNull(routerResponse.getQueue())) {
            supportTicket.setAssignedQueue(
                routerResponse.getQueue()
            );
        }

        supportTicket.setLatestRoutingConfidence(routerResponse.getConfidence());
        supportTicket.setLatestRoutingVersion(supportTicket.getLatestRoutingVersion() + 1);

        supportTicket.addRouting(
            buildRouting(
                supportTicket,
                routerResponse,
                chooseWithFallback(
                    routerResponse.getQueue(),
                    supportTicket.getAssignedQueue(),
                    TicketQueue.GENERAL_Q
                )
            )
        );

        supportTicketRepository.save(supportTicket);

        log.info(
            "StateMachineRoutingApply({}) SupportTicket(id:{},status:{},queue:{},priority:{}) RouterResponse(confidence:{})",
            OperationalLogContext.PHASE_PERSIST,
            OperationalLogContext.ticketId(supportTicket),
            OperationalLogContext.status(supportTicket),
            OperationalLogContext.queue(supportTicket),
            OperationalLogContext.priority(supportTicket),
            routerResponse.getConfidence()
        );
    }

    private void forceEscalation(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        String escalationReason = autonomousProgressService.getEscalationReason(
            supportTicket
        );

        TicketAutonomousMetadata autonomousMetadata = Objects.requireNonNullElse(
            supportTicket.getAutonomousMetadata(),
            TicketAutonomousMetadata.builder().build()
        );

        autonomousMetadata.setEscalationReason(escalationReason);

        supportTicket.setAutonomousMetadata(autonomousMetadata);

        if (Objects.nonNull(routerResponse.getCategory())) {
            supportTicket.setCurrentCategory(routerResponse.getCategory());
        }

        if (Objects.nonNull(routerResponse.getPriority())) {
            supportTicket.setCurrentPriority(routerResponse.getPriority());
        }

        supportTicket.setAssignedQueue(
            Objects.requireNonNullElse(
                routerResponse.getQueue(),
                TicketQueue.GENERAL_Q
            )
        );

        supportTicket.setLatestRoutingConfidence(routerResponse.getConfidence());
        supportTicket.setLatestRoutingVersion(supportTicket.getLatestRoutingVersion() + 1);

        supportTicket.addRouting(
            buildRouting(
                supportTicket,
                routerResponse,
                chooseWithFallback(
                    routerResponse.getQueue(),
                    TicketQueue.GENERAL_Q
                )
            )
        );

        supportTicket.setStatus(TicketStatus.ESCALATED);
        supportTicket.setEscalated(true);
        supportTicket.setRequiresHumanReview(false);

        supportTicketRepository.save(supportTicket);

        log.warn(
            "StateMachineForceEscalate({}) SupportTicket(id:{},status:{},queue:{}) Outcome(reason:{})",
            OperationalLogContext.PHASE_PERSIST,
            OperationalLogContext.ticketId(supportTicket),
            OperationalLogContext.status(supportTicket),
            OperationalLogContext.queue(supportTicket),
            escalationReason
        );
    }

    private static TicketRouting buildRouting(
        SupportTicket supportTicket,
        RouterResponse routerResponse,
        TicketQueue resolvedQueue
    ) {
        TicketCategory category = chooseWithFallback(
            routerResponse.getCategory(),
            supportTicket.getCurrentCategory(),
            TicketCategory.OTHER
        );

        TicketPriority priority = chooseWithFallback(
            routerResponse.getPriority(),
            supportTicket.getCurrentPriority(),
            TicketPriority.MEDIUM
        );

        NextAction nextAction = chooseWithFallback(
            routerResponse.getNextAction(),
            NextAction.HUMAN_REVIEW
        );

        List<String> rationaleTags = copyRationaleTagsOrEmpty(
            routerResponse.getRationaleTags()
        );

        return TicketRouting.builder()
                            .version(supportTicket.getLatestRoutingVersion())
                            .category(category)
                            .priority(priority)
                            .queue(resolvedQueue)
                            .nextAction(nextAction)
                            .confidence(routerResponse.getConfidence())
                            .clarifyingQuestion(routerResponse.getClarifyingQuestion())
                            .draftReply(routerResponse.getDraftReply())
                            .rationaleTags(rationaleTags)
                            .applied(true)
                            .build();
    }

    @SafeVarargs
    private static <T> T chooseWithFallback(
        T... candidates
    ) {
        for (T candidate : candidates) {
            if (Objects.nonNull(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static List<String> copyRationaleTagsOrEmpty(
        List<String> tags
    ) {
        return Objects.nonNull(tags) ? new ArrayList<>(tags) : new ArrayList<>();
    }
}
