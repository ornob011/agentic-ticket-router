package com.dsi.support.agenticrouter.dto;

import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
public class TicketAnalysisRequest {

    @NotNull(message = "Ticket ID is required")
    private Long ticketId;

    @NotNull(message = "Content is required")
    private String content;
}
