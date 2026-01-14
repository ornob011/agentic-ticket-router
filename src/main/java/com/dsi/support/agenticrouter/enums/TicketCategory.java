package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum TicketCategory {
    BILLING("Billing and payment related issues"),
    TECHNICAL("Technical problems and bugs"),
    ACCOUNT("Account management and access"),
    SHIPPING("Shipping and delivery queries"),
    SECURITY("Security concerns and threats"),
    OTHER("General inquiries and other topics");

    private static final Set<TicketCategory> HIGH_RISK =
        EnumSet.of(
            SECURITY
        );

    private static final Set<TicketCategory> CUSTOMER_ACCOUNT_RELATED =
        EnumSet.of(
            ACCOUNT,
            BILLING
        );

    private final String description;

    TicketCategory(String description) {
        this.description = description;
    }

    public boolean isHighRisk() {
        return HIGH_RISK.contains(this);
    }

    public boolean isAccountOrBilling() {
        return CUSTOMER_ACCOUNT_RELATED.contains(this);
    }
}
