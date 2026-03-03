package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.CreateTicketDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketCreationCommandService {

    private final SupportTicketRepository supportTicketRepository;
    private final AppUserRepository appUserRepository;
    private final TicketCreationWorkflowService ticketCreationWorkflowService;

    public SupportTicket createTicket(
        CreateTicketDto createTicketDto,
        Long customerId
    ) {
        log.info(
            "TicketCreate({}) Actor(id:{}) Outcome(subjectLength:{},contentLength:{})",
            OperationalLogContext.PHASE_START,
            customerId,
            StringUtils.length(StringNormalizationUtils.trimToNull(createTicketDto.getSubject())),
            StringUtils.length(StringNormalizationUtils.trimToNull(createTicketDto.getContent()))
        );

        AppUser customer = appUserRepository.findById(customerId)
                                            .orElseThrow(
                                                DataNotFoundException.supplier(
                                                    AppUser.class,
                                                    customerId
                                                )
                                            );

        SupportTicket supportTicket = SupportTicket.builder()
                                                   .customer(customer)
                                                   .subject(createTicketDto.getSubject())
                                                   .status(TicketStatus.RECEIVED)
                                                   .lastActivityAt(Instant.now())
                                                   .build();

        supportTicket = supportTicketRepository.save(supportTicket);

        log.info(
            "TicketCreate({}) SupportTicket(id:{},ticketNo:{},status:{},queue:{},priority:{}) Actor(id:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getFormattedTicketNo(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            supportTicket.getCurrentPriority(),
            customerId
        );

        applyPostCreateWorkflows(
            supportTicket,
            customer,
            createTicketDto
        );

        log.info(
            "TicketCreate({}) SupportTicket(id:{},ticketNo:{},status:{}) Outcome(event:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getFormattedTicketNo(),
            supportTicket.getStatus(),
            "post_create_workflow_applied"
        );

        return supportTicket;
    }

    private void applyPostCreateWorkflows(
        SupportTicket supportTicket,
        AppUser customer,
        CreateTicketDto createTicketDto
    ) {
        ticketCreationWorkflowService.applyPostCreateWorkflows(
            supportTicket,
            customer,
            createTicketDto,
            this
        );
    }
}
