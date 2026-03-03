package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

@Getter
public enum ToolName {
    AUTO_REPLY("auto_reply", "Generate and send automated reply to customer"),
    ASK_CLARIFYING("ask_clarifying", "Request clarifying information from customer"),
    ASSIGN_QUEUE("assign_queue", "Assign ticket to appropriate queue"),
    ESCALATE("escalate", "Escalate ticket to supervisor or specialized team"),
    HUMAN_REVIEW("human_review", "Mark ticket for human review"),
    UPDATE_CUSTOMER_PROFILE("update_customer_profile", "Update customer profile information"),
    CHANGE_PRIORITY("change_priority", "Change ticket priority"),
    ADD_INTERNAL_NOTE("add_internal_note", "Add internal note for agents"),
    AUTO_ESCALATE("auto_escalate", "Auto-escalate ticket to supervisor"),
    AUTO_RESOLVE("auto_resolve", "Auto-resolve ticket with solution"),
    REOPEN_TICKET("reopen_ticket", "Reopen closed or resolved ticket"),
    TRIGGER_NOTIFICATION("trigger_notification", "Send notification to relevant parties"),
    USE_KNOWLEDGE_ARTICLE("use_knowledge_article", "Use knowledge base article for resolution"),
    USE_TEMPLATE("use_template", "Use response template"),
    GET_TICKET_INFO("get_ticket_info", "Retrieve current ticket information"),
    GET_CUSTOMER_HISTORY("get_customer_history", "Retrieve customer ticket history"),
    SEARCH_KNOWLEDGE_BASE("search_knowledge_base", "Search knowledge base for relevant articles"),


    ;

    private final String functionName;
    private final String description;

    ToolName(
        String functionName,
        String description
    ) {
        this.functionName = functionName;
        this.description = description;
    }

    public static ToolName fromNextAction(
        NextAction nextAction
    ) {
        return switch (nextAction) {
            case AUTO_REPLY -> AUTO_REPLY;
            case ASK_CLARIFYING -> ASK_CLARIFYING;
            case ASSIGN_QUEUE -> ASSIGN_QUEUE;
            case ESCALATE -> ESCALATE;
            case HUMAN_REVIEW -> HUMAN_REVIEW;
            case UPDATE_CUSTOMER_PROFILE -> UPDATE_CUSTOMER_PROFILE;
            case CHANGE_PRIORITY -> CHANGE_PRIORITY;
            case ADD_INTERNAL_NOTE -> ADD_INTERNAL_NOTE;
            case AUTO_ESCALATE -> AUTO_ESCALATE;
            case AUTO_RESOLVE -> AUTO_RESOLVE;
            case REOPEN_TICKET -> REOPEN_TICKET;
            case TRIGGER_NOTIFICATION -> TRIGGER_NOTIFICATION;
            case USE_KNOWLEDGE_ARTICLE -> USE_KNOWLEDGE_ARTICLE;
            case USE_TEMPLATE -> USE_TEMPLATE;
        };
    }

    public NextAction toNextAction() {
        return switch (this) {
            case AUTO_REPLY -> NextAction.AUTO_REPLY;
            case ASK_CLARIFYING -> NextAction.ASK_CLARIFYING;
            case ASSIGN_QUEUE -> NextAction.ASSIGN_QUEUE;
            case ESCALATE -> NextAction.ESCALATE;
            case HUMAN_REVIEW -> NextAction.HUMAN_REVIEW;
            case UPDATE_CUSTOMER_PROFILE -> NextAction.UPDATE_CUSTOMER_PROFILE;
            case CHANGE_PRIORITY -> NextAction.CHANGE_PRIORITY;
            case ADD_INTERNAL_NOTE -> NextAction.ADD_INTERNAL_NOTE;
            case AUTO_ESCALATE -> NextAction.AUTO_ESCALATE;
            case AUTO_RESOLVE -> NextAction.AUTO_RESOLVE;
            case REOPEN_TICKET -> NextAction.REOPEN_TICKET;
            case TRIGGER_NOTIFICATION -> NextAction.TRIGGER_NOTIFICATION;
            case USE_KNOWLEDGE_ARTICLE -> NextAction.USE_KNOWLEDGE_ARTICLE;
            case USE_TEMPLATE -> NextAction.USE_TEMPLATE;
            default -> throw new IllegalArgumentException(
                String.format(
                    "ToolName %s has no corresponding NextAction",
                    this
                )
            );
        };
    }
}
