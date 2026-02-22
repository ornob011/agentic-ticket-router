package com.dsi.support.agenticrouter.service.ai;

import com.dsi.support.agenticrouter.enums.AgentOrchestrationMode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptService {

    private final Map<AgentOrchestrationMode, Resource> plannerPromptByMode = new EnumMap<>(AgentOrchestrationMode.class);

    private final Map<AgentOrchestrationMode, Resource> repairPromptByMode = new EnumMap<>(AgentOrchestrationMode.class);

    @Value("classpath:/prompts/system.st")
    private Resource systemPromptResource;

    @Value("classpath:/prompts/routing.st")
    private Resource routingPromptResource;

    @Value("classpath:/prompts/repair.st")
    private Resource repairPromptResource;

    @Value("classpath:/prompts/category_detection.st")
    private Resource categoryDetectionPromptResource;

    @Value("classpath:/prompts/agent_planner.st")
    private Resource agentPlannerPromptResource;

    @Value("classpath:/prompts/agent_repair.st")
    private Resource agentRepairPromptResource;

    @Value("classpath:/prompts/single_agent_planner.st")
    private Resource singleAgentPlannerPromptResource;

    @Value("classpath:/prompts/single_agent_repair.st")
    private Resource singleAgentRepairPromptResource;

    @Value("classpath:/prompts/multi_agent_planner.st")
    private Resource multiAgentPlannerPromptResource;

    @Value("classpath:/prompts/multi_agent_repair.st")
    private Resource multiAgentRepairPromptResource;

    @Value("classpath:/prompts/schemas/router_response.schema.json")
    private Resource routerResponseSchemaResource;

    @Value("classpath:/prompts/ticket_reply_draft.st")
    private Resource ticketReplyDraftPromptResource;

    @Value("classpath:/prompts/ticket_reply_draft_system.st")
    private Resource ticketReplyDraftSystemPromptResource;

    @PostConstruct
    void initializeModePromptMappings() {
        plannerPromptByMode.put(
            AgentOrchestrationMode.SINGLE_AGENT,
            singleAgentPlannerPromptResource
        );
        plannerPromptByMode.put(
            AgentOrchestrationMode.MULTI_AGENT,
            multiAgentPlannerPromptResource
        );
        repairPromptByMode.put(
            AgentOrchestrationMode.SINGLE_AGENT,
            singleAgentRepairPromptResource
        );
        repairPromptByMode.put(
            AgentOrchestrationMode.MULTI_AGENT,
            multiAgentRepairPromptResource
        );
    }

    public Resource getSystemPrompt() {
        return systemPromptResource;
    }

    public Resource getRoutingPrompt() {
        return routingPromptResource;
    }

    public Resource getRepairPrompt() {
        return repairPromptResource;
    }

    public Resource getCategoryDetectionPrompt() {
        return categoryDetectionPromptResource;
    }

    public Resource getAgentPlannerPrompt() {
        return agentPlannerPromptResource;
    }

    public Resource getAgentRepairPrompt() {
        return agentRepairPromptResource;
    }

    public Resource getPlannerPromptByFlowMode(
        AgentOrchestrationMode flowMode
    ) {
        return plannerPromptByMode.get(flowMode);
    }

    public Resource getPlannerRepairPromptByFlowMode(
        AgentOrchestrationMode flowMode
    ) {
        return repairPromptByMode.get(flowMode);
    }

    public Resource getRouterResponseSchema() {
        return routerResponseSchemaResource;
    }

    public Resource getTicketReplyDraftPrompt() {
        return ticketReplyDraftPromptResource;
    }

    public Resource getTicketReplyDraftSystemPrompt() {
        return ticketReplyDraftSystemPromptResource;
    }
}
