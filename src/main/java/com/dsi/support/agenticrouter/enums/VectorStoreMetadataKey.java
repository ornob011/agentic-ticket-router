package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

@Getter
public enum VectorStoreMetadataKey {

    ARTICLE_ID("ID of the knowledge base article"),
    CATEGORY("Article category"),
    PRIORITY("Article priority"),
    ARTICLE_TYPE("Type of the article (SOLUTION, FAQ, etc.)"),
    ACTIVE("Whether the article is active"),
    KEYWORDS("Keywords associated with the article"),


    ;

    private final String description;

    VectorStoreMetadataKey(String description) {
        this.description = description;
    }
}
