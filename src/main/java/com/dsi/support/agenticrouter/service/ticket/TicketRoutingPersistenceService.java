package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketRouting;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TicketRoutingPersistenceService {

    private final SupportTicketRepository supportTicketRepository;

    public void applyRoutingDecision(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        if (Objects.nonNull(routerResponse.getCategory())) {
            supportTicket.setCurrentCategory(
                routerResponse.getCategory()
            );
        }

        if (Objects.nonNull(routerResponse.getPriority())) {
            supportTicket.setCurrentPriority(
                routerResponse.getPriority()
            );
        }

        if (Objects.nonNull(routerResponse.getQueue())) {
            supportTicket.setAssignedQueue(
                routerResponse.getQueue()
            );
        }

        supportTicket.setLatestRoutingConfidence(
            routerResponse.getConfidence()
        );
        supportTicket.setLatestRoutingVersion(
            supportTicket.getLatestRoutingVersion() + 1
        );

        supportTicket.addRouting(
            buildRouting(
                supportTicket,
                routerResponse,
                chooseWithFallback(
                    routerResponse.getQueue(),
                    supportTicket.getAssignedQueue(),
                    TicketQueue.GENERAL_Q
                )
            )
        );

        supportTicketRepository.save(
            supportTicket
        );
    }

    private TicketRouting buildRouting(
        SupportTicket supportTicket,
        RouterResponse routerResponse,
        TicketQueue resolvedQueue
    ) {
        TicketCategory category = chooseWithFallback(
            routerResponse.getCategory(),
            supportTicket.getCurrentCategory(),
            TicketCategory.OTHER
        );

        TicketPriority priority = chooseWithFallback(
            routerResponse.getPriority(),
            supportTicket.getCurrentPriority(),
            TicketPriority.MEDIUM
        );

        NextAction nextAction = chooseWithFallback(
            routerResponse.getNextAction(),
            NextAction.HUMAN_REVIEW
        );

        List<String> rationaleTags = copyRationaleTagsOrEmpty(
            routerResponse.getRationaleTags()
        );

        return TicketRouting.builder()
                            .version(supportTicket.getLatestRoutingVersion())
                            .category(category)
                            .priority(priority)
                            .queue(resolvedQueue)
                            .nextAction(nextAction)
                            .confidence(routerResponse.getConfidence())
                            .clarifyingQuestion(routerResponse.getClarifyingQuestion())
                            .draftReply(routerResponse.getDraftReply())
                            .rationaleTags(rationaleTags)
                            .applied(true)
                            .build();
    }

    @SafeVarargs
    private static <T> T chooseWithFallback(
        T... candidates
    ) {
        for (T candidate : candidates) {
            if (Objects.nonNull(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static List<String> copyRationaleTagsOrEmpty(
        List<String> tags
    ) {
        return Objects.nonNull(tags) ? new ArrayList<>(tags) : new ArrayList<>();
    }
}
