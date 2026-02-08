package com.dsi.support.agenticrouter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolveEscalationDto {

    @NotBlank(message = "{escalation.resolution.notes.required}")
    private String resolutionNotes;
}

