package com.dsi.support.agenticrouter.dto;

import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouterResponse {

    @NotNull(message = "Category is required")
    @JsonProperty("category")
    private TicketCategory category;

    @NotNull(message = "Priority is required")
    @JsonProperty("priority")
    private TicketPriority priority;

    @NotNull(message = "Queue is required")
    @JsonProperty("queue")
    private TicketQueue queue;

    @NotNull(message = "Next action is required")
    @JsonProperty("next_action")
    private NextAction nextAction;

    @NotNull(message = "Confidence is required")
    @DecimalMin(value = "0.0", message = "Confidence must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Confidence must be between 0 and 1")
    @JsonProperty("confidence")
    private BigDecimal confidence;

    @JsonProperty("clarifying_question")
    private String clarifyingQuestion;

    @JsonProperty("draft_reply")
    private String draftReply;

    @JsonProperty("rationale_tags")
    @Builder.Default
    private List<String> rationaleTags = new ArrayList<>();

    @JsonProperty("action_parameters")
    private Map<String, Object> actionParameters;

    @JsonProperty("internal_note")
    private String internalNote;
}
