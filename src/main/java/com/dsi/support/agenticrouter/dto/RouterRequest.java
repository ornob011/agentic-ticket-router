package com.dsi.support.agenticrouter.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RouterRequest {

    private Long ticketId;

    private String ticketNo;

    private String subject;

    private String customerName;

    private String customerTier;

    private String initialMessage;

    private String conversationHistory;

    private String latestCustomerMessage;

    private String latestAssistantMessage;

    private String previousClarifyingQuestion;

    private List<ArticleSearchResult> relevantArticles;

    private List<PatternHint> relevantPatterns;

    private Integer remainingActions;

    private Integer questionsAsked;

    private Integer maxQuestions;

    private Integer maxActions;
}
