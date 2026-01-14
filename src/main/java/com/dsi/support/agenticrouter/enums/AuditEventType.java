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
    PRIORITY_CHANGED("Priority changed"),
    NOTIFICATION_SENT("Notification sent to user"),
    SLA_BREACH("SLA threshold breached"),
    AUTO_CLOSE_TRIGGERED("Auto-close triggered"),
    TICKET_REOPENED("Ticket reopened"),
    POLICY_GATE_TRIGGERED("Policy gate triggered"),
    MODEL_INFERENCE_FAILED("Model inference failed"),
    MANUAL_INTERVENTION("Manual intervention by staff");

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

    private final String description;

    AuditEventType(String description) {
        this.description = description;
    }

    public boolean isSystemGenerated() {
        return SYSTEM_GENERATED.contains(this);
    }

    public boolean isErrorEvent() {
        return ERROR_EVENTS.contains(this);
    }
}
