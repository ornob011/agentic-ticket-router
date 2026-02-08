package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

@Getter
public enum ArticleType {
    SOLUTION("Direct solution for known issues"),
    FAQ("Common questions and answers"),
    PROCEDURAL_GUIDE("Step-by-step instructions"),
    TROUBLESHOOTING("Problem resolution steps"),
    QUICK_SOLUTION("Quick fix articles"),
    FEATURE_OVERVIEW("Feature overview and announcements"),

    ;

    private final String description;

    ArticleType(String description) {
        this.description = description;
    }
}
