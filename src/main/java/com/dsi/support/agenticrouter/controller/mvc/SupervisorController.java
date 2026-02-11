package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.dto.ResolveEscalationDto;
import com.dsi.support.agenticrouter.entity.Escalation;
import com.dsi.support.agenticrouter.enums.EscalationFilterStatus;
import com.dsi.support.agenticrouter.enums.NavPage;
import com.dsi.support.agenticrouter.service.TicketService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SupervisorController {

    private final TicketService ticketService;
    private final MessageSource messageSource;

    @GetMapping("/supervisor/escalations")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    public String listEscalations(
        @RequestParam(required = false) EscalationFilterStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Model model
    ) {
        log.info(
            "EscalationList({}) Actor(id:{}) Outcome(statusFilter:{},page:{},size:{})",
            OperationalLogContext.PHASE_START,
            Utils.getLoggedInUserId(),
            status,
            page,
            size
        );

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by("createdAt").descending()
        );

        Page<Escalation> escalations;

        switch (status) {
            case RESOLVED -> escalations = ticketService.listEscalationsByResolved(
                true,
                pageable
            );

            case PENDING -> escalations = ticketService.listEscalationsByResolved(
                false,
                pageable
            );

            default -> escalations = ticketService.listAllEscalations(
                pageable
            );
        }

        model.addAttribute("escalations", escalations);
        model.addAttribute("currentPage", NavPage.ESCALATIONS);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", page);

        log.info(
            "EscalationList({}) Actor(id:{}) Outcome(resultCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            Utils.getLoggedInUserId(),
            escalations.getNumberOfElements()
        );

        return "supervisor/escalations";
    }

    @PostMapping("/supervisor/escalations/{escalationId}/resolve")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    public String resolveEscalation(
        @PathVariable Long escalationId,
        @Valid @ModelAttribute("resolveEscalationDto") ResolveEscalationDto resolveEscalationDto,
        BindingResult bindingResult,
        Model model,
        HttpServletRequest request
    ) {
        log.info(
            "EscalationResolve({}) Escalation(id:{}) Actor(id:{}) Outcome(notesLength:{})",
            OperationalLogContext.PHASE_START,
            escalationId,
            Utils.getLoggedInUserId(),
            StringUtils.length(StringUtils.trimToNull(resolveEscalationDto.getResolutionNotes()))
        );

        if (bindingResult.hasErrors()) {
            log.warn(
                "EscalationResolve({}) Escalation(id:{}) Actor(id:{}) Outcome(validationErrors:{})",
                OperationalLogContext.PHASE_FAIL,
                escalationId,
                Utils.getLoggedInUserId(),
                bindingResult.getErrorCount()
            );

            Escalation escalation = ticketService.getEscalationById(escalationId);

            model.addAttribute("escalation", escalation);
            model.addAttribute("resolveEscalationDto", resolveEscalationDto);
            model.addAttribute("currentPage", NavPage.ESCALATIONS);

            return "supervisor/escalation-detail";
        }

        ticketService.resolveEscalation(
            escalationId,
            resolveEscalationDto.getResolutionNotes()
        );

        log.info(
            "EscalationResolve({}) Escalation(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            escalationId,
            Utils.getLoggedInUserId()
        );

        Utils.setSuccessMessageCode(
            request,
            messageSource,
            "escalation.resolve.success"
        );

        return "redirect:/supervisor/escalations";
    }

    @GetMapping("/supervisor/escalations/{escalationId}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    public String viewEscalation(
        @PathVariable Long escalationId,
        Model model
    ) {
        log.info(
            "EscalationDetailView({}) Escalation(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            escalationId,
            Utils.getLoggedInUserId()
        );

        Escalation escalation = ticketService.getEscalationById(escalationId);
        model.addAttribute("escalation", escalation);
        model.addAttribute("resolveEscalationDto", new ResolveEscalationDto());
        model.addAttribute("currentPage", NavPage.ESCALATIONS);

        log.info(
            "EscalationDetailView({}) Escalation(id:{}) SupportTicket(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            escalation.getId(),
            escalation.getTicket().getId()
        );

        return "supervisor/escalation-detail";
    }
}
