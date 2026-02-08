package com.dsi.support.agenticrouter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketDto {

    private String subject;

    private String content;
}
