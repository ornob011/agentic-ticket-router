package com.dsi.support.agenticrouter.prompts;

import com.dsi.support.agenticrouter.enums.TicketAnalysis;
import com.dsi.support.agenticrouter.resource.analysis.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Service
public class TicketAnalysisPrompts {

    private final TicketDetailsResource ticketDetailsResource;
    private final CustomerInformationResource customerInformationResource;
    private final ConversationHistoryResource conversationHistoryResource;
    private final TechnicalDetailsResource technicalDetailsResource;
    private final ActionsRequiredResource actionsRequiredResource;

    @Value("classpath:/prompts/system/analyzer.st")
    private Resource analyzerSystemMsg;

    @Value("classpath:/prompts/base-ticket-analyzer.st")
    private Resource baseTicketAnalyzerPrompt;

    Map<TicketAnalysis, TicketAnalysisResource> ticketAnalysisResourceMap;

    public TicketAnalysisPrompts(
        TicketDetailsResource ticketDetailsResource,
        CustomerInformationResource customerInformationResource,
        ConversationHistoryResource conversationHistoryResource,
        TechnicalDetailsResource technicalDetailsResource,
        ActionsRequiredResource actionsRequiredResource
    ) {
        ticketAnalysisResourceMap = new EnumMap<>(TicketAnalysis.class);

        ticketAnalysisResourceMap.put(
            TicketAnalysis.TICKET_DETAILS,
            ticketDetailsResource
        );

        ticketAnalysisResourceMap.put(
            TicketAnalysis.CUSTOMER_INFORMATION,
            customerInformationResource
        );

        ticketAnalysisResourceMap.put(
            TicketAnalysis.CONVERSATION_HISTORY,
            conversationHistoryResource
        );

        ticketAnalysisResourceMap.put(
            TicketAnalysis.TECHNICAL_DETAILS,
            technicalDetailsResource
        );

        ticketAnalysisResourceMap.put(
            TicketAnalysis.ACTIONS_REQUIRED,
            actionsRequiredResource
        );

        this.ticketDetailsResource = ticketDetailsResource;
        this.customerInformationResource = customerInformationResource;
        this.conversationHistoryResource = conversationHistoryResource;
        this.technicalDetailsResource = technicalDetailsResource;
        this.actionsRequiredResource = actionsRequiredResource;
    }

    public Resource getAnalysisTemplate(
        TicketAnalysis ticketAnalysis
    ) {

        TicketAnalysisResource ticketAnalysisResource = ticketAnalysisResourceMap.get(
            ticketAnalysis
        );

        if (Objects.isNull(ticketAnalysisResource) || Objects.isNull(ticketAnalysisResource.getAnalysisPrompt())) {
            return baseTicketAnalyzerPrompt;
        }

        return ticketAnalysisResource.getAnalysisPrompt();
    }

    public Resource getMarkdownTemplate(
        TicketAnalysis ticketAnalysis
    ) {

        TicketAnalysisResource ticketAnalysisResource = ticketAnalysisResourceMap.get(
            ticketAnalysis
        );

        if (Objects.isNull(ticketAnalysisResource) || Objects.isNull(ticketAnalysisResource.getAnalysisMarkdown())) {
            throw new UnsupportedOperationException("Section analysis markdown not implemented for " + ticketAnalysis);
        }

        return ticketAnalysisResource.getAnalysisMarkdown();
    }
}
