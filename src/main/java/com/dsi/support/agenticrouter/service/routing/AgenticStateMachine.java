package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketAutonomousMetadata;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.AutonomousProgressService;
import com.dsi.support.agenticrouter.service.action.ActionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (autonomousProgressService.shouldEscalate(supportTicket)) {
            forceEscalation(
                supportTicket,
                routerResponse
            );
            return;
        }

        actionRegistry.execute(
            supportTicket,
            routerResponse
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
    }
}
