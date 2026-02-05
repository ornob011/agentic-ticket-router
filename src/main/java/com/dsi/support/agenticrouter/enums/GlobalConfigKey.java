package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

@Getter
public enum GlobalConfigKey {

    VECTOR_STORE_INITIALIZED("Whether vector store has been initialized with seed data"),
    VECTOR_STORE_VERSION("Version of the vector store schema/data"),
    KB_SEED_VERSION("Version of the knowledge base seed data"),
    SYSTEM_DEPLOYMENT_VERSION("Current system deployment version"),


    ;

    private final String description;

    GlobalConfigKey(String description) {
        this.description = description;
    }
}
