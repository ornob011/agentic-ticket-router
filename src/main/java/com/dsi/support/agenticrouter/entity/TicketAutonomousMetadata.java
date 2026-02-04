package com.dsi.support.agenticrouter.entity;

import lombok.*;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAutonomousMetadata {

    @Builder.Default
    private int autonomousActionCount = 0;

    @Builder.Default
    private int questionCount = 0;

    private Instant firstAutonomousActionAt;
    private Instant lastAutonomousActionAt;

    private String lastClarifyingQuestion;
    private Instant lastClarifyingQuestionAt;

    @Builder.Default
    private Deque<String> recentQuestions = new ArrayDeque<>(5);

    @Builder.Default
    private boolean hasFrustrationDetected = false;

    @Setter
    private String escalationReason;

    public void incrementActionCount() {
        autonomousActionCount++;
        Instant now = Instant.now();
        lastAutonomousActionAt = now;

        if (Objects.isNull(firstAutonomousActionAt)) {
            firstAutonomousActionAt = now;
        }
    }

    public void incrementQuestionCount() {
        questionCount++;
    }

    public void recordClarifyingQuestion(
        String question
    ) {
        Instant now = Instant.now();
        lastClarifyingQuestion = question;
        lastClarifyingQuestionAt = now;

        questionCount++;

        recentQuestions.addLast(question);

        while (recentQuestions.size() > 5) {
            recentQuestions.removeFirst();
        }
    }

    public boolean shouldContinue(
        int maxActions,
        int maxQuestions
    ) {
        return autonomousActionCount < maxActions
               && questionCount < maxQuestions
               && !hasFrustrationDetected;
    }

    public boolean isRepeatingQuestion(
        String question
    ) {
        return recentQuestions.stream()
                              .anyMatch(incomingQuestion -> Objects.equals(incomingQuestion, question));
    }
}
