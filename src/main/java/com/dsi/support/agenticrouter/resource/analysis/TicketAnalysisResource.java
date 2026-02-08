package com.dsi.support.agenticrouter.resource.analysis;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

@Getter
public abstract class TicketAnalysisResource {

    @Value("classpath:/prompts/base-ticket-analyzer.st")
    protected Resource analysisPrompt;

    @Value("classpath:/prompts/ticket-analyzer.md")
    protected Resource analysisMarkdown;

    public abstract Resource getAnalysisPrompt();

    public abstract Resource getAnalysisMarkdown();
}
