package com.dsi.support.agenticrouter.dto;

import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDetailDto {

    private Long id;
    private Long ticketNo;
    private String formattedTicketNo;
    private String subject;
    private TicketStatus status;
    private TicketCategory currentCategory;
    private TicketPriority currentPriority;
    private TicketQueue assignedQueue;

    // Customer info
    private Long customerId;
    private String customerName;
    private String customerEmail;

    // Agent info
    private Long assignedAgentId;
    private String assignedAgentName;

    // Timestamps
    private Instant createdAt;
    private Instant lastActivityAt;
    private Instant resolvedAt;
    private Instant closedAt;

    // Routing info
    private BigDecimal latestRoutingConfidence;
    private Integer latestRoutingVersion;

    // Flags
    private boolean escalated;
    private int reopenCount;

    // Derived states
    private boolean requiresCustomerAction;
    private boolean requiresAgentAction;
    private boolean isTerminal;
}
