package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

@Getter
public enum VectorStoreMetadataKey {

    ARTICLE_ID("ID of the knowledge base article"),
    CATEGORY("Article category"),
    PRIORITY("Article priority"),
    ARTICLE_TYPE("Type of the article (SOLUTION, FAQ, etc.)"),
    TITLE("Article title"),
    ACTIVE("Whether the article is active"),
    KEYWORDS("Keywords associated with the article"),
    CHUNK_ID("Knowledge chunk ID"),
    CHUNK_INDEX("Position of chunk in the article"),
    CHUNK_TYPE("Chunk type"),
    ARTICLE_VERSION("Version of the article represented by chunk"),
    SECTION_PATH("Section path for chunk"),
    CONTENT_HASH("Content hash for chunk"),


    ;

    private final String description;

    VectorStoreMetadataKey(String description) {
        this.description = description;
    }
}
