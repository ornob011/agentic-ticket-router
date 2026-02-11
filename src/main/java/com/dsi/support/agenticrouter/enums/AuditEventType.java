package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum AuditEventType {
    TICKET_CREATED("Ticket created"),
    TICKET_STATUS_CHANGED("Ticket status changed"),
    MESSAGE_POSTED("Message posted to ticket"),
    ROUTING_EXECUTED("AI routing executed"),
    ROUTING_OVERRIDDEN("Routing decision overridden by human"),
    QUEUE_ASSIGNED("Ticket assigned to queue"),
    AGENT_ASSIGNED("Ticket assigned to specific agent"),
    ESCALATION_CREATED("Escalation created"),
    ESCALATION_RESOLVED("Escalation resolved"),
    PRIORITY_CHANGED("Priority changed"),
    NOTIFICATION_SENT("Notification sent to user"),
    SLA_BREACH("SLA threshold breached"),
    AUTO_CLOSE_TRIGGERED("Auto-close triggered"),
    TICKET_REOPENED("Ticket reopened"),
    POLICY_GATE_TRIGGERED("Policy gate triggered"),
    MODEL_INFERENCE_FAILED("Model inference failed"),
    MANUAL_INTERVENTION("Manual intervention by staff"),
    TICKET_ANALYSIS_EXECUTED("Ticket analysis executed"),
    TICKET_ANALYSIS_FAILED("Ticket analysis failed"),
    TEMPLATE_AUTO_SELECTED("Template auto-selected by system");

    private static final Set<AuditEventType> SYSTEM_GENERATED =
        EnumSet.of(
            ROUTING_EXECUTED,
            AUTO_CLOSE_TRIGGERED,
            SLA_BREACH,
            NOTIFICATION_SENT
        );

    private static final Set<AuditEventType> ERROR_EVENTS =
        EnumSet.of(
            MODEL_INFERENCE_FAILED
        );

    private static final Set<AuditEventType> CUSTOMER_VISIBLE =
        EnumSet.of(
            QUEUE_ASSIGNED,
            AGENT_ASSIGNED,
            ESCALATION_CREATED,
            PRIORITY_CHANGED,
            TICKET_STATUS_CHANGED,
            TICKET_REOPENED
        );

    private final String description;

    AuditEventType(String description) {
        this.description = description;
    }

    public static Set<AuditEventType> getCustomerVisible() {
        return CUSTOMER_VISIBLE;
    }

    public boolean isSystemGenerated() {
        return SYSTEM_GENERATED.contains(this);
    }

    public boolean isErrorEvent() {
        return ERROR_EVENTS.contains(this);
    }

    public boolean isCustomerVisible() {
        return CUSTOMER_VISIBLE.contains(this);
    }
}
