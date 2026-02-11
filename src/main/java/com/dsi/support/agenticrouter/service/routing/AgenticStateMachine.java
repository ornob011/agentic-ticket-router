package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketAutonomousMetadata;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.AutonomousProgressService;
import com.dsi.support.agenticrouter.service.action.ActionRegistry;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    ) {
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

        supportTicket.setLatestRoutingConfidence(
            routerResponse.getConfidence()
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

        TicketAutonomousMetadata autonomousMetadata = supportTicket.getAutonomousMetadata();

        autonomousMetadata.setEscalationReason(escalationReason);

        supportTicket.setAutonomousMetadata(autonomousMetadata);
        supportTicket.setStatus(TicketStatus.AUTO_ESCALATED);
        supportTicket.setAssignedQueue(routerResponse.getQueue());

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
}
