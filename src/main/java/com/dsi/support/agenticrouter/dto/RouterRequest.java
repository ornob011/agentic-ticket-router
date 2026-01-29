package com.dsi.support.agenticrouter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouterRequest {

    private Long ticketId;

    private String ticketNo;

    private String subject;

    private String customerName;

    private String customerTier;

    private String initialMessage;

    private String conversationHistory;
}
