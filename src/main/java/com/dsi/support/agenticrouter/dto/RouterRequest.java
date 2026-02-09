package com.dsi.support.agenticrouter.dto;

import com.dsi.support.agenticrouter.enums.TicketCategory;
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

    private String analysis;

    private TicketCategory suggestedCategory;

    private String previousClarifyingQuestion;

    private List<ArticleSearchResult> relevantArticles;

    private Integer remainingActions;

    private Integer questionsAsked;

    private Integer maxQuestions;

    private Integer maxActions;
}
