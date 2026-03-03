package com.dsi.support.agenticrouter.service.agentruntime.orchestration;

import com.dsi.support.agenticrouter.configuration.AgentRuntimeConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AgentRuntimeTerminationPolicy {

    private final AgentRuntimeConfiguration agentRuntimeConfiguration;

    public boolean shouldTerminate(
        AgentGraphState agentGraphState
    ) {
        if (agentGraphState.getStepCount() >= agentRuntimeConfiguration.getMaxSteps()) {
            return true;
        }

        Instant executionStartedAt = agentGraphState.getStartedAt();

        long elapsedMs = Duration.between(
            executionStartedAt,
            Instant.now()
        ).toMillis();

        return elapsedMs >= agentRuntimeConfiguration.getMaxRuntimeMs();
    }
}
