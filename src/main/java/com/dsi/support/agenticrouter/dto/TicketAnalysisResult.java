package com.dsi.support.agenticrouter.dto;

import com.dsi.support.agenticrouter.enums.TicketAnalysis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAnalysisResult {

    private TicketAnalysis section;
    private String extractedMarkdown;
    private Double confidence;
}
