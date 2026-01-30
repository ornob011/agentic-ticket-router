package com.dsi.support.agenticrouter.resource.analysis;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ConversationHistoryResource extends TicketAnalysisResource {

    @Override
    public Resource getAnalysisPrompt() {
        return analysisPrompt;
    }

    @Override
    public Resource getAnalysisMarkdown() {
        return analysisMarkdown;
    }
}
