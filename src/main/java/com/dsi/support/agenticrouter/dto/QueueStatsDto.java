package com.dsi.support.agenticrouter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class QueueStatsDto {

    private Long assignedCount;
    private Long inProgressCount;
    private Long resolvedCount;
    private Long escalatedCount;
    private Long awaitingCustomerCount;
    private Long triagingCount;
}
