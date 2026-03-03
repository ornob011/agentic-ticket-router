package com.dsi.support.agenticrouter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class NotificationDto {

    private Long id;
    private String title;
    private String body;
    private String type;
    private Long ticketId;
    private String link;
    private Boolean read;
    private Instant createdAt;
}
