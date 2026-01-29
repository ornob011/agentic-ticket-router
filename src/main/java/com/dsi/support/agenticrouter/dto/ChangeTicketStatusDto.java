package com.dsi.support.agenticrouter.dto;

import com.dsi.support.agenticrouter.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeTicketStatusDto {

    @NotNull(message = "{ticket.status.required}")
    private TicketStatus newStatus;

    @Size(max = 500, message = "{ticket.status.reason.max}")
    private String reason;
}

