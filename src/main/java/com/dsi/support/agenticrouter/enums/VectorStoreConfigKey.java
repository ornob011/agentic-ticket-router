package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

@Getter
public enum VectorStoreConfigKey {

    INITIALIZED("Whether vector store has been initialized"),
    VERSION("Version of the vector store"),
    KB_SEED_VERSION("Version of the knowledge base seed data"),
    SYNC_TIMESTAMP("Last timestamp of the vector store sync"),
    KB_ARTICLE_COUNT("Total count of the knowledge base articles"),


    ;

    private final String description;

    VectorStoreConfigKey(String description) {
        this.description = description;
    }
}
