package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.dto.CreateTicketDto;
import com.dsi.support.agenticrouter.dto.TicketReplyDto;
import com.dsi.support.agenticrouter.entity.AuditEvent;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.NavPage;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.TicketService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import com.dsi.support.agenticrouter.validator.CreateTicketValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;
    private final MessageSource messageSource;
    private final CreateTicketValidator createTicketValidator;
    private final AuditService auditService;

    @InitBinder("createTicketDto")
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(createTicketValidator);
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String listCustomerTickets(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Model model
    ) {
        Long customerId = Utils.getLoggedInUserId();

        log.info(
            "TicketList({}) Actor(id:{}) Outcome(page:{},size:{})",
            OperationalLogContext.PHASE_START,
            customerId,
            page,
            size
        );

        Pageable pageable = PageRequest.of(page, size);

        Page<SupportTicket> supportTickets = ticketService.listCustomerTickets(
            customerId,
            pageable
        );

        model.addAttribute("currentPage", NavPage.TICKETS);
        model.addAttribute("tickets", supportTickets);

        log.info(
            "TicketList({}) Actor(id:{}) Outcome(resultCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            customerId,
            supportTickets.getNumberOfElements()
        );

        return "tickets/list";
    }

    @GetMapping("/create/new")
    public String showCreateForm(Model model) {
        model.addAttribute("createTicketDto", new CreateTicketDto());
        model.addAttribute("currentPage", NavPage.NEW_TICKET);

        return "tickets/create";
    }

    @PostMapping("/create/new")
    public String createTicket(
        @Valid @ModelAttribute CreateTicketDto createTicketDto,
        BindingResult bindingResult,
        HttpServletRequest request,
        Model model
    ) {
        log.info(
            "TicketCreate({}) Actor(id:{}) Outcome(subjectLength:{},contentLength:{})",
            OperationalLogContext.PHASE_START,
            Utils.getLoggedInUserId(),
            StringUtils.length(createTicketDto.getSubject()),
            StringUtils.length(createTicketDto.getContent())
        );

        if (bindingResult.hasErrors()) {
            log.warn(
                "TicketCreate({}) Actor(id:{}) Outcome(validationErrors:{})",
                OperationalLogContext.PHASE_FAIL,
                Utils.getLoggedInUserId(),
                bindingResult.getErrorCount()
            );

            model.addAttribute("createTicketDto", createTicketDto);
            model.addAttribute("currentPage", NavPage.NEW_TICKET);

            return "tickets/create";
        }

        Long customerId = Utils.getLoggedInUserId();

        ticketService.createTicket(
            createTicketDto,
            customerId
        );

        log.info(
            "TicketCreate({}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            customerId
        );

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "success.ticket.created"
        );

        return "redirect:/tickets";
    }

    @GetMapping("/{ticketId}")
    public String viewTicket(
        @PathVariable Long ticketId,
        Model model
    ) {
        log.info(
            "TicketDetailView({}) SupportTicket(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            Utils.getLoggedInUserId()
        );

        SupportTicket supportTicket = ticketService.getTicket(
            ticketId
        );

        List<TicketMessage> ticketMessages = ticketService.getTicketMessages(
            ticketId
        );

        List<AuditEvent> auditEvents = auditService.getTicketAuditTrail(
            ticketId
        );

        model.addAttribute("ticket", supportTicket);
        model.addAttribute("messages", ticketMessages);
        model.addAttribute("auditEvents", auditEvents);
        model.addAttribute("replyDto", new TicketReplyDto());

        log.info(
            "TicketDetailView({}) SupportTicket(id:{},status:{}) Outcome(messageCount:{},auditCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            ticketMessages.size(),
            auditEvents.size()
        );

        return "tickets/detail";
    }

    @PostMapping("/{ticketId}/reply")
    public String addReply(
        @PathVariable Long ticketId,
        @ModelAttribute("replyDto") TicketReplyDto replyDto,
        BindingResult bindingResult,
        Model model,
        HttpServletRequest request
    ) {
        Long customerId = Utils.getLoggedInUserId();

        log.info(
            "CustomerReply({}) SupportTicket(id:{}) Actor(id:{}) Outcome(contentLength:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            customerId,
            StringUtils.length(replyDto.getContent())
        );

        if (StringUtils.isBlank(replyDto.getContent())) {
            log.warn(
                "CustomerReply({}) SupportTicket(id:{}) Actor(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_FAIL,
                ticketId,
                customerId,
                "empty_content"
            );

            bindingResult.rejectValue("content", "reply.content.required");

            SupportTicket supportTicket = ticketService.getTicket(
                ticketId
            );

            List<TicketMessage> ticketMessages = ticketService.getTicketMessages(
                ticketId
            );

            model.addAttribute("ticket", supportTicket);
            model.addAttribute("messages", ticketMessages);

            return "tickets/detail";
        }

        ticketService.addCustomerReply(
            ticketId,
            replyDto.getContent(),
            customerId
        );

        log.info(
            "CustomerReply({}) SupportTicket(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            customerId
        );

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "success.reply.added"
        );

        String redirectUrl = UriComponentsBuilder.fromPath("/tickets/{ticketId}")
                                                 .buildAndExpand(ticketId)
                                                 .toUriString();


        return "redirect:" + redirectUrl;
    }

}
