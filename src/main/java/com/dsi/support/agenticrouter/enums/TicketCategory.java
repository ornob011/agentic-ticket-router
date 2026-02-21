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
    PRICING("Pricing plans and fee information"),
    OTHER("General inquiries and other topics"),
    CANCEL("Cancellation procedures"),
    CONTACT("Contact and support details"),
    DELIVERY("Delivery and shipping info"),
    FEEDBACK("User feedback and surveys"),
    INVOICE("Invoice and billing details"),
    ORDER("Order processing and status"),
    PAYMENT("Payment methods and issues"),
    REFUND("Refund policies and procedures"),
    SUBSCRIPTION("Subscription plans and management"),


    ;

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

    public TicketCategory toRoutingCategory() {
        return switch (this) {
            case PAYMENT, INVOICE, REFUND, SUBSCRIPTION -> BILLING;
            case DELIVERY, ORDER -> SHIPPING;
            case CANCEL -> ACCOUNT;
            case CONTACT, FEEDBACK -> OTHER;
            default -> this;
        };
    }
}
