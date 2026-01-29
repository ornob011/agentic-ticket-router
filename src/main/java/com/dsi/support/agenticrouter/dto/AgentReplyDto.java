package com.dsi.support.agenticrouter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentReplyDto {

    @NotBlank(message = "{reply.content.required}")
    private String content;
}
