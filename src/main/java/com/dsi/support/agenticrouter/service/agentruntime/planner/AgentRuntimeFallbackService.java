package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.util.AgentRuntimeConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AgentRuntimeFallbackService {

    public RouterResponse humanReviewFallback(
        String reason
    ) {
        return RouterResponse.builder()
                             .category(TicketCategory.OTHER)
                             .priority(TicketPriority.MEDIUM)
                             .queue(TicketQueue.GENERAL_Q)
                             .nextAction(NextAction.HUMAN_REVIEW)
                             .confidence(BigDecimal.ZERO)
                             .clarifyingQuestion(null)
                             .draftReply(null)
                             .internalNote(reason)
                             .rationaleTags(List.of(AgentRuntimeConstants.FALLBACK_RATIONALE_TAG))
                             .build();
    }
}
