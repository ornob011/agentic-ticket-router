package com.dsi.support.agenticrouter.service.agentruntime.trace;

import com.dsi.support.agenticrouter.entity.AgentRuntimeRun;
import com.dsi.support.agenticrouter.entity.AgentRuntimeStep;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AgentRuntimeRunStatus;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AgentRuntimeRunRepository;
import com.dsi.support.agenticrouter.repository.AgentRuntimeStepRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.util.AgentRuntimeConstants;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentRuntimeTraceService {

    private final AgentRuntimeRunRepository agentRuntimeRunRepository;
    private final AgentRuntimeStepRepository agentRuntimeStepRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgentRuntimeRun startRun(
        Long ticketId
    ) {
        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        AgentRuntimeRun run = AgentRuntimeRun.builder()
                                             .ticket(supportTicket)
                                             .correlationId(MDC.get(AgentRuntimeConstants.CORRELATION_ID_MDC_KEY))
                                             .status(AgentRuntimeRunStatus.RUNNING)
                                             .startedAt(Instant.now())
                                             .build();

        run = agentRuntimeRunRepository.save(run);

        log.info(
            "AgentTrace({}) SupportTicket(id:{}) AgentRun(id:{},status:{})",
            OperationalLogContext.PHASE_PERSIST,
            ticketId,
            run.getId(),
            run.getStatus()
        );

        return run;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordStep(
        AgentRuntimeStepTraceCommand command
    ) {
        AgentRuntimeRun run = agentRuntimeRunRepository.findById(command.runId())
                                                       .orElseThrow(
                                                           DataNotFoundException.supplier(
                                                               AgentRuntimeRun.class,
                                                               command.runId()
                                                           )
                                                       );

        AgentRuntimeStep step = AgentRuntimeStep.builder()
                                                .run(run)
                                                .stepNo(command.stepNo())
                                                .stepType(command.stepType())
                                                .plannerOutput(toJson(command.plannerOutput()))
                                                .validatedResponse(toJson(command.validatedResponse()))
                                                .safetyDecision(toJson(command.safetyDecision()))
                                                .toolResult(toJson(command.toolResult()))
                                                .latencyMs(command.latencyMs())
                                                .success(command.success())
                                                .errorCode(command.errorCode())
                                                .errorMessage(command.errorMessage())
                                                .build();

        agentRuntimeStepRepository.save(step);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishRun(
        AgentRuntimeRunFinishCommand command
    ) {
        AgentRuntimeRun run = agentRuntimeRunRepository.findById(command.runId())
                                                       .orElseThrow(
                                                           DataNotFoundException.supplier(
                                                               AgentRuntimeRun.class,
                                                               command.runId()
                                                           )
                                                       );

        run.setStatus(command.status());
        run.setTerminationReason(command.terminationReason());
        run.setTotalSteps(command.totalSteps());
        run.setFallbackUsed(command.fallbackUsed());
        run.setErrorCode(command.errorCode());
        run.setErrorMessage(command.errorMessage());
        run.setEndedAt(Instant.now());

        agentRuntimeRunRepository.save(run);

        log.info(
            "AgentTrace({}) AgentRun(id:{},status:{},termination:{},steps:{})",
            OperationalLogContext.PHASE_COMPLETE,
            run.getId(),
            run.getStatus(),
            run.getTerminationReason(),
            run.getTotalSteps()
        );
    }

    private JsonNode toJson(
        Object value
    ) {
        if (Objects.isNull(value)) {
            return null;
        }

        return objectMapper.valueToTree(value);
    }
}
