package com.dsi.support.agenticrouter.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptService {

    @Value("classpath:/prompts/system.st")
    private Resource systemPromptResource;

    @Value("classpath:/prompts/routing.st")
    private Resource routingPromptResource;

    @Value("classpath:/prompts/analysis.st")
    private Resource analysisPromptResource;

    @Value("classpath:/prompts/repair.st")
    private Resource repairPromptResource;

    @Value("classpath:/prompts/category_detection.st")
    private Resource categoryDetectionPromptResource;

    public Resource getSystemPrompt() {
        return systemPromptResource;
    }

    public Resource getRoutingPrompt() {
        return routingPromptResource;
    }

    public Resource getAnalysisPrompt() {
        return analysisPromptResource;
    }

    public Resource getRepairPrompt() {
        return repairPromptResource;
    }

    public Resource getCategoryDetectionPrompt() {
        return categoryDetectionPromptResource;
    }
}
