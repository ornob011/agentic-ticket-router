package com.dsi.support.agenticrouter.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouterRequest {

    private Long ticketId;

    private String ticketNo;

    private String subject;

    private String customerName;

    private String customerTier;

    private String initialMessage;

    private String conversationHistory;

    private String analysis;
}
