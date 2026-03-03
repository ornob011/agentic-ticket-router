package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.GraphStateException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouterOrchestrator {

    private final RoutingExecutionCoordinator routingExecutionCoordinator;
    private final SupportTicketRepository supportTicketRepository;
    private final RoutingRequestFactory routingRequestFactory;

    @Async("ticketRoutingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void routeTicket(
        Long ticketId
    ) throws BindException, GraphStateException {
        log.info(
            "TicketRoute({}) SupportTicket(id:{})",
            OperationalLogContext.PHASE_START,
            ticketId
        );

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        supportTicket.setStatus(TicketStatus.TRIAGING);
        supportTicket = supportTicketRepository.save(supportTicket);

        log.info(
            "TicketRoute({}) SupportTicket(id:{},ticketNo:{},status:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getFormattedTicketNo(),
            supportTicket.getStatus()
        );

        RouterRequest routerRequest = buildRouterRequest(
            supportTicket
        );

        RoutingExecutionResult routingExecutionResult = routingExecutionCoordinator.execute(
            supportTicket,
            routerRequest
        );

        log.info(
            "TicketRoute({}) SupportTicket(id:{},status:{}) Outcome(source:{},nextAction:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            routingExecutionResult.source(),
            routingExecutionResult.response().getNextAction()
        );
    }

    private RouterRequest buildRouterRequest(
        SupportTicket supportTicket
    ) {
        return routingRequestFactory.buildRouterRequest(
            supportTicket
        );
    }
}
