package com.dsi.support.agenticrouter.enums;

public enum RoutingPolicyRuleCode {
    SECURITY_CONTENT_ESCALATION("security_content_escalation"),
    CRITICAL_MIN_CONF("critical_min_conf"),
    AUTO_ROUTE_THRESHOLD("auto_route_threshold");

    private final String code;

    RoutingPolicyRuleCode(
        String code
    ) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
