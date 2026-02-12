package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

@Getter
public enum KnowledgeChunkType {
    TITLE("Article title"),
    SECTION_HEADER("Section header"),
    BODY("Body paragraph"),
    FAQ_Q("FAQ question"),
    FAQ_A("FAQ answer"),
    TABLE_ROW("Table row"),


    ;

    private final String description;

    KnowledgeChunkType(String description) {
        this.description = description;
    }
}
