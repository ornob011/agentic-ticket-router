package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.List;

@Getter
public enum RoutingActionParameterKey {
    TEMPLATE_ID("template_id"),
    ARTICLE_ID("article_id"),
    PHONE_NUMBER("phone_number"),
    COMPANY_NAME("company_name"),
    ADDRESS("address"),
    CITY("city"),
    POSTAL_CODE("postal_code"),
    PREFERRED_LANGUAGE_CODE("preferred_language_code"),


    ;

    private final String key;

    RoutingActionParameterKey(
        String key
    ) {
        this.key = key;
    }

    public static List<RoutingActionParameterKey> profileUpdateFields() {
        return List.of(
            PHONE_NUMBER,
            COMPANY_NAME,
            ADDRESS,
            CITY,
            POSTAL_CODE,
            PREFERRED_LANGUAGE_CODE
        );
    }
}
