package com.dsi.support.agenticrouter.configuration;

import com.dsi.support.agenticrouter.enums.TicketQueue;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "agent.runtime")
public class AgentRuntimeConfiguration {

    private boolean enabled = true;
    private boolean shadowMode = false;
    private boolean canaryEnabled = false;

    private List<TicketQueue> allowedQueues = List.of();

    private boolean schemaEnforcementEnabled = true;
    private boolean repairEnabled = true;
    private boolean providerStructuredOutputEnabled = true;

    @Min(0)
    private int plannerValidationRetries = 1;

    @Min(1)
    private int maxSteps = 4;

    @Min(1)
    private long maxRuntimeMs = 15000L;
}
