package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.CreateTicketDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

@Service
@Transactional
@RequiredArgsConstructor
public class TicketLifecycleCommandService {

    private final TicketCreationCommandService ticketCreationCommandService;
    private final TicketReplyCommandService ticketReplyCommandService;
    private final TicketStatusCommandService ticketStatusCommandService;
    private final TicketRoutingCommandService ticketRoutingCommandService;

    public SupportTicket createTicket(
        CreateTicketDto createTicketDto,
        Long customerId
    ) {
        return ticketCreationCommandService.createTicket(
            createTicketDto,
            customerId
        );
    }

    public void addCustomerReply(
        Long ticketId,
        String content,
        Long customerId
    ) throws BindException {
        ticketReplyCommandService.addCustomerReply(
            ticketId,
            content,
            customerId
        );
    }

    public void addAgentReply(
        Long ticketId,
        String content
    ) throws BindException {
        ticketReplyCommandService.addAgentReply(
            ticketId,
            content
        );
    }

    public void addAgentReply(
        Long ticketId,
        String content,
        AppUser agent,
        String businessDriver
    ) throws BindException {
        ticketReplyCommandService.addAgentReply(
            ticketId,
            content,
            agent,
            businessDriver
        );
    }

    public void changeTicketStatus(
        Long ticketId,
        TicketStatus targetStatus,
        String reason
    ) throws BindException {
        ticketStatusCommandService.changeTicketStatus(
            ticketId,
            targetStatus,
            reason
        );
    }

    public void changeTicketStatus(
        Long ticketId,
        TicketStatus targetStatus,
        String businessDriver,
        String reason
    ) throws BindException {
        ticketStatusCommandService.changeTicketStatus(
            ticketId,
            targetStatus,
            businessDriver,
            reason
        );
    }

    public void overrideRouting(
        Long ticketId,
        TicketQueue newQueue,
        TicketPriority newPriority,
        String reason
    ) throws BindException {
        ticketRoutingCommandService.overrideRouting(
            ticketId,
            newQueue,
            newPriority,
            reason
        );
    }
}
