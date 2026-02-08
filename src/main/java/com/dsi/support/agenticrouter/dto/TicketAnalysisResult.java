package com.dsi.support.agenticrouter.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketAnalysisResult {

    private String analysis;

    private Double confidence;
}
