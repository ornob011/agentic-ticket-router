package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.model.TicketAutonomousMetadata;
import com.dsi.support.agenticrouter.service.policy.PolicyValueLookupService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutonomousProgressService {

    private final PolicyValueLookupService policyValueLookupService;

    private int getMaxAutonomousActions() {
        int maxAutonomousActions = policyValueLookupService.getRequiredIntValue(
            PolicyConfigKey.MAX_AUTONOMOUS_ACTIONS
        );

        return Math.max(
            maxAutonomousActions,
            1
        );
    }

    private int getMaxQuestions() {
        int maxQuestions = policyValueLookupService.getRequiredIntValue(
            PolicyConfigKey.MAX_QUESTIONS_PER_TICKET
        );

        return Math.max(
            maxQuestions,
            1
        );
    }

    @Transactional
    public void trackAutonomousAction(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        log.debug(
            "AutonomousTrack({}) SupportTicket(id:{},status:{}) RouterResponse(nextAction:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus(),
            routerResponse.getNextAction()
        );

        supportTicket.incrementAutonomousActionCount();

        if (NextAction.ASK_CLARIFYING.equals(routerResponse.getNextAction())) {
            String question = routerResponse.getClarifyingQuestion();

            if (StringUtils.isNotBlank(question)) {
                supportTicket.recordClarifyingQuestion(question);
            }
        }

        log.debug(
            "AutonomousTrack({}) SupportTicket(id:{}) Outcome(actionCount:{},questionCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getAutonomousActionCount(),
            supportTicket.getQuestionCount()
        );
    }

    public boolean shouldContinueAutonomous(
        SupportTicket supportTicket
    ) {
        TicketAutonomousMetadata autonomousMetadata = metadataOrDefault(
            supportTicket
        );
        boolean shouldContinue = autonomousMetadata.shouldContinue(
            getMaxAutonomousActions(),
            getMaxQuestions()
        );

        log.debug(
            "AutonomousContinueDecision({}) SupportTicket(id:{},status:{}) Outcome(shouldContinue:{},actionCount:{},questionCount:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            supportTicket.getStatus(),
            shouldContinue,
            autonomousMetadata.getAutonomousActionCount(),
            autonomousMetadata.getQuestionCount()
        );

        return shouldContinue;
    }

    public boolean shouldEscalate(
        SupportTicket supportTicket
    ) {
        return !shouldContinueAutonomous(supportTicket)
               || hasLoopDetected(supportTicket)
               || hasFrustration(supportTicket);
    }

    private boolean hasLoopDetected(
        SupportTicket supportTicket
    ) {
        TicketAutonomousMetadata autonomousMetadata = metadataOrDefault(
            supportTicket
        );

        return autonomousMetadata.getRecentQuestions().size() >= 3
               && autonomousMetadata.getRecentQuestions()
                                    .stream()
                                    .distinct()
                                    .count() <= 1;
    }

    private boolean hasFrustration(
        SupportTicket supportTicket
    ) {
        return supportTicket.hasFrustrationDetected();
    }

    public String getEscalationReason(
        SupportTicket supportTicket
    ) {
        TicketAutonomousMetadata autonomousMetadata = metadataOrDefault(
            supportTicket
        );

        if (autonomousMetadata.isHasFrustrationDetected()) {
            return "Customer frustration detected - escalating to human";
        }

        if (autonomousMetadata.getQuestionCount() >= getMaxQuestions()) {
            return String.format("Max questions (%d) reached - escalating", getMaxQuestions());
        }

        if (autonomousMetadata.getAutonomousActionCount() >= getMaxAutonomousActions()) {
            return String.format("Max autonomous actions (%d) reached - escalating", getMaxAutonomousActions());
        }

        if (hasLoopDetected(supportTicket)) {
            return "Repeating question pattern detected - escalating to human";
        }

        return "Autonomous limit reached - escalating to human";
    }

    private TicketAutonomousMetadata metadataOrDefault(
        SupportTicket supportTicket
    ) {
        return Objects.requireNonNullElseGet(
            supportTicket.getAutonomousMetadata(),
            () -> TicketAutonomousMetadata.builder().build()
        );
    }
}
