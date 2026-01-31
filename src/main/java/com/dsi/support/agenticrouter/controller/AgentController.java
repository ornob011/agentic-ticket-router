package com.dsi.support.agenticrouter.controller;

import com.dsi.support.agenticrouter.dto.AgentReplyDto;
import com.dsi.support.agenticrouter.dto.ChangeTicketStatusDto;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.NavPage;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.TicketService;
import com.dsi.support.agenticrouter.util.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final TicketService ticketService;
    private final MessageSource messageSource;
    private final SupportTicketRepository supportTicketRepository;

    @GetMapping("/agent/tickets/{ticketId}")
    public String viewTicket(
        @PathVariable Long ticketId,
        Model model
    ) {
        SupportTicket ticket = ticketService.getTicket(ticketId);

        model.addAttribute("ticket", ticket);
        model.addAttribute("messages", ticketService.getTicketMessages(ticketId));
        model.addAttribute("availableStatuses", TicketStatus.values());

        return "agent/ticket-detail";
    }

    @PostMapping("/agent/tickets/{ticketId}/reply")
    public String addReply(
        @PathVariable Long ticketId,
        @ModelAttribute("replyDto") AgentReplyDto agentReplyDto,
        BindingResult bindingResult,
        Model model,
        HttpServletRequest request
    ) {
        if (!StringUtils.hasText(agentReplyDto.getContent())) {
            bindingResult.rejectValue("content", "reply.content.required");

            SupportTicket supportTicket = ticketService.getTicket(ticketId);
            List<TicketMessage> ticketMessages = ticketService.getTicketMessages(ticketId);

            model.addAttribute("ticket", supportTicket);
            model.addAttribute("messages", ticketMessages);

            return "agent/ticket-detail";
        }

        ticketService.addAgentReply(
            ticketId,
            agentReplyDto.getContent()
        );

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "success.ticket.reply.added"
        );

        return "redirect:/agent/tickets/" + ticketId;
    }

    @PostMapping("/agent/tickets/{ticketId}/status")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    public String changeStatus(
        @PathVariable Long ticketId,
        @Valid @ModelAttribute("changeStatusDto") ChangeTicketStatusDto changeTicketStatusDto,
        BindingResult bindingResult,
        Model model,
        HttpServletRequest request
    ) {
        if (bindingResult.hasErrors()) {
            SupportTicket supportTicket = ticketService.getTicket(ticketId);
            List<TicketMessage> ticketMessages = ticketService.getTicketMessages(ticketId);

            model.addAttribute("ticket", supportTicket);
            model.addAttribute("messages", ticketMessages);
            model.addAttribute("changeStatusDto", changeTicketStatusDto);

            return "agent/ticket-detail";
        }

        ticketService.changeTicketStatus(
            ticketId,
            changeTicketStatusDto.getNewStatus(),
            changeTicketStatusDto.getReason()
        );

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "ticket.status.change.success"
        );

        return "redirect:/agent/tickets/" + ticketId;
    }

    @GetMapping("/agent/queue/{queue}")
    public String queueInbox(
        @PathVariable TicketQueue queue,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Model model
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SupportTicket> tickets = ticketService.listQueueTickets(
            queue,
            null,
            pageable
        );

        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedQueue", queue);
        model.addAttribute("selectedStatus", null);
        model.addAttribute("availableQueues", TicketQueue.values());
        model.addAttribute("availableStatuses", TicketStatus.values());
        model.addAttribute("currentPage", page);

        return "agent/queue";
    }

    @PostMapping("/agent/tickets/{ticketId}/assign")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    public String assignTicket(
        @PathVariable Long ticketId,
        @RequestParam Long agentId,
        HttpServletRequest request
    ) {
        ticketService.assignAgent(ticketId, agentId);

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "ticket.assign.success"
        );

        return "redirect:/agent/queue/" + ticketService.getTicket(ticketId).getAssignedQueue();
    }

    @PostMapping("/agent/tickets/{ticketId}/release")
    public String releaseTicket(
        @PathVariable Long ticketId,
        HttpServletRequest request
    ) {
        ticketService.releaseAgent(ticketId);

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "ticket.release.success"
        );

        return "redirect:/agent/queue/" + ticketService.getTicket(ticketId).getAssignedQueue();
    }

    @GetMapping("/agent/unassigned/{queue}")
    public String viewUnassigned(
        @PathVariable TicketQueue queue,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Model model
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SupportTicket> tickets = supportTicketRepository.findUnassignedTicketsInQueue(
            queue,
            List.of(TicketStatus.ASSIGNED),
            pageable
        );

        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedQueue", queue);
        model.addAttribute("availableQueues", TicketQueue.values());
        model.addAttribute("currentPage", page);

        return "agent/queue";
    }

    @GetMapping("/agent/dashboard")
    public String agentDashboard(
        @RequestParam(required = false) TicketQueue queue,
        @RequestParam(required = false) TicketStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Model model
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SupportTicket> tickets = ticketService.listQueueTickets(
            queue,
            status,
            pageable
        );

        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedQueue", queue);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("availableQueues", TicketQueue.values());
        model.addAttribute("availableStatuses", TicketStatus.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("currentPage", NavPage.QUEUE);

        return "agent/queue";
    }
}
