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

        AgentRuntimeRun agentRuntimeRun = AgentRuntimeRun.builder()
                                                         .ticket(supportTicket)
                                                         .correlationId(MDC.get(AgentRuntimeConstants.CORRELATION_ID_MDC_KEY))
                                                         .status(AgentRuntimeRunStatus.RUNNING)
                                                         .startedAt(Instant.now())
                                                         .build();

        agentRuntimeRun = agentRuntimeRunRepository.save(agentRuntimeRun);

        log.info(
            "AgentTrace({}) SupportTicket(id:{}) AgentRun(id:{},status:{})",
            OperationalLogContext.PHASE_PERSIST,
            ticketId,
            agentRuntimeRun.getId(),
            agentRuntimeRun.getStatus()
        );

        return agentRuntimeRun;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordStep(
        AgentRuntimeStepTraceCommand stepTraceCommand
    ) {
        AgentRuntimeRun agentRuntimeRun = agentRuntimeRunRepository.findById(stepTraceCommand.runId())
                                                                   .orElseThrow(
                                                                       DataNotFoundException.supplier(
                                                                           AgentRuntimeRun.class,
                                                                           stepTraceCommand.runId()
                                                                       )
                                                                   );

        AgentRuntimeStep agentRuntimeStep = AgentRuntimeStep.builder()
                                                            .run(agentRuntimeRun)
                                                            .stepNo(stepTraceCommand.stepNo())
                                                            .stepType(stepTraceCommand.stepType())
                                                            .plannerOutput(toJson(stepTraceCommand.plannerOutput()))
                                                            .validatedResponse(toJson(stepTraceCommand.validatedResponse()))
                                                            .safetyDecision(toJson(stepTraceCommand.safetyDecision()))
                                                            .toolResult(toJson(stepTraceCommand.toolResult()))
                                                            .latencyMs(stepTraceCommand.latencyMs())
                                                            .success(stepTraceCommand.success())
                                                            .errorCode(stepTraceCommand.errorCode())
                                                            .errorMessage(stepTraceCommand.errorMessage())
                                                            .build();

        agentRuntimeStepRepository.save(agentRuntimeStep);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishRun(
        AgentRuntimeRunFinishCommand runFinishCommand
    ) {
        AgentRuntimeRun agentRuntimeRun = agentRuntimeRunRepository.findById(runFinishCommand.runId())
                                                                   .orElseThrow(
                                                                       DataNotFoundException.supplier(
                                                                           AgentRuntimeRun.class,
                                                                           runFinishCommand.runId()
                                                                       )
                                                                   );

        agentRuntimeRun.setStatus(runFinishCommand.status());
        agentRuntimeRun.setTerminationReason(runFinishCommand.terminationReason());
        agentRuntimeRun.setTotalSteps(runFinishCommand.totalSteps());
        agentRuntimeRun.setFallbackUsed(runFinishCommand.fallbackUsed());
        agentRuntimeRun.setErrorCode(runFinishCommand.errorCode());
        agentRuntimeRun.setErrorMessage(runFinishCommand.errorMessage());
        agentRuntimeRun.setEndedAt(Instant.now());

        agentRuntimeRunRepository.save(agentRuntimeRun);

        log.info(
            "AgentTrace({}) AgentRun(id:{},status:{},termination:{},steps:{})",
            OperationalLogContext.PHASE_COMPLETE,
            agentRuntimeRun.getId(),
            agentRuntimeRun.getStatus(),
            agentRuntimeRun.getTerminationReason(),
            agentRuntimeRun.getTotalSteps()
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
