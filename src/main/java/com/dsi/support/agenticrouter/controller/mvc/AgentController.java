package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.dto.AgentReplyDto;
import com.dsi.support.agenticrouter.dto.ChangeTicketStatusDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.entity.TicketRouting;
import com.dsi.support.agenticrouter.enums.NavPage;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.TicketService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
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
    private final AppUserRepository appUserRepository;

    @GetMapping("/agent/tickets/{ticketId}")
    public String viewTicket(
        @PathVariable Long ticketId,
        Model model
    ) {
        log.info(
            "AgentTicketView({}) SupportTicket(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            Utils.getLoggedInUserId()
        );

        SupportTicket ticket = ticketService.getTicket(ticketId);
        List<TicketMessage> ticketMessages = ticketService.getTicketMessages(ticketId);

        List<TicketRouting> routingHistory = ticketService.getTicketRoutingHistory(ticketId);
        TicketRouting latestRouting = routingHistory.isEmpty() ? null : routingHistory.get(0);

        List<AppUser> availableAgents = appUserRepository.findByActiveTrue();

        model.addAttribute("ticket", ticket);
        model.addAttribute("messages", ticketMessages);
        model.addAttribute("latestRouting", latestRouting);
        model.addAttribute("routingHistory", routingHistory);
        model.addAttribute("availableStatuses", TicketStatus.values());
        model.addAttribute("availableQueues", TicketQueue.values());
        model.addAttribute("availablePriorities", TicketPriority.values());
        model.addAttribute("availableAgents", availableAgents);

        log.info(
            "AgentTicketView({}) SupportTicket(id:{},status:{},queue:{},priority:{}) Outcome(messageCount:{},routingHistoryCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId(),
            ticket.getStatus(),
            ticket.getAssignedQueue(),
            ticket.getCurrentPriority(),
            ticketMessages.size(),
            routingHistory.size()
        );

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
        log.info(
            "AgentReply({}) SupportTicket(id:{}) Actor(id:{}) Outcome(contentLength:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            Utils.getLoggedInUserId(),
            StringUtils.hasText(agentReplyDto.getContent()) ? agentReplyDto.getContent().trim().length() : 0
        );

        if (!StringUtils.hasText(agentReplyDto.getContent())) {
            log.warn(
                "AgentReply({}) SupportTicket(id:{}) Actor(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_FAIL,
                ticketId,
                Utils.getLoggedInUserId(),
                "empty_content"
            );

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

        log.info(
            "AgentReply({}) SupportTicket(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            Utils.getLoggedInUserId()
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
        log.info(
            "TicketStatusChange({}) SupportTicket(id:{}) Actor(id:{}) Outcome(targetStatus:{},reasonLength:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            Utils.getLoggedInUserId(),
            changeTicketStatusDto.getNewStatus(),
            StringUtils.hasText(changeTicketStatusDto.getReason()) ? changeTicketStatusDto.getReason().trim().length() : 0
        );

        if (bindingResult.hasErrors()) {
            log.warn(
                "TicketStatusChange({}) SupportTicket(id:{}) Actor(id:{}) Outcome(validationErrors:{})",
                OperationalLogContext.PHASE_FAIL,
                ticketId,
                Utils.getLoggedInUserId(),
                bindingResult.getErrorCount()
            );

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

        log.info(
            "TicketStatusChange({}) SupportTicket(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            Utils.getLoggedInUserId()
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
        log.info(
            "QueueInbox({}) Actor(id:{}) Outcome(queue:{},page:{},size:{})",
            OperationalLogContext.PHASE_START,
            Utils.getLoggedInUserId(),
            queue,
            page,
            size
        );

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

        log.info(
            "QueueInbox({}) Actor(id:{}) Outcome(queue:{},resultCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            Utils.getLoggedInUserId(),
            queue,
            tickets.getNumberOfElements()
        );

        return "agent/queue";
    }

    @PostMapping("/agent/tickets/{ticketId}/assign")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    public String assignTicket(
        @PathVariable Long ticketId,
        @RequestParam Long agentId,
        HttpServletRequest request
    ) {
        log.info(
            "AgentAssign({}) SupportTicket(id:{}) Actor(id:{}) Outcome(targetAgentId:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            Utils.getLoggedInUserId(),
            agentId
        );

        ticketService.assignAgent(ticketId, agentId);

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "ticket.assign.success"
        );

        log.info(
            "AgentAssign({}) SupportTicket(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            Utils.getLoggedInUserId()
        );

        return "redirect:/agent/queue/" + ticketService.getTicket(ticketId).getAssignedQueue();
    }

    @PostMapping("/agent/tickets/{ticketId}/release")
    public String releaseTicket(
        @PathVariable Long ticketId,
        HttpServletRequest request
    ) {
        log.info(
            "AgentRelease({}) SupportTicket(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            Utils.getLoggedInUserId()
        );

        ticketService.releaseAgent(ticketId);

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "ticket.release.success"
        );

        log.info(
            "AgentRelease({}) SupportTicket(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            Utils.getLoggedInUserId()
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

    @GetMapping("/agent/review-queue")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    public String reviewQueue(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Model model
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SupportTicket> triagingTickets = supportTicketRepository.findByStatusAndAssignedQueueIsNull(
            TicketStatus.TRIAGING,
            pageable
        );

        model.addAttribute("tickets", triagingTickets);
        model.addAttribute("currentPage", NavPage.REVIEW_QUEUE);
        model.addAttribute("availablePriorities", TicketPriority.values());
        model.addAttribute("availableQueues", TicketQueue.values());
        model.addAttribute("currentPage", page);

        return "agent/review-queue";
    }
}
