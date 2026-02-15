package com.dsi.support.agenticrouter.service.knowledge;

import com.dsi.support.agenticrouter.entity.KnowledgeArticle;
import com.dsi.support.agenticrouter.enums.VectorStoreMetadataKey;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class KnowledgeArticleVectorDocumentMapper {

    private static final String DOCUMENT_ID_PREFIX = "kb_article_";

    public Document toDocument(
        KnowledgeArticle article
    ) {
        return new Document(
            documentId(article),
            renderContent(article),
            buildMetadata(article)
        );
    }

    private String documentId(
        KnowledgeArticle article
    ) {
        String sourceId = DOCUMENT_ID_PREFIX + article.getId();

        return UUID.nameUUIDFromBytes(sourceId.getBytes(StandardCharsets.UTF_8))
                   .toString();
    }

    private Map<String, Object> buildMetadata(
        KnowledgeArticle article
    ) {
        Map<String, Object> metadata = new HashMap<>(8);

        metadata.put(
            VectorStoreMetadataKey.ARTICLE_ID.name(),
            article.getId()
        );
        metadata.put(
            VectorStoreMetadataKey.ACTIVE.name(),
            article.getActive()
        );

        Optional.ofNullable(article.getCategory())
                .map(Enum::name)
                .ifPresent(value -> metadata.put(
                    VectorStoreMetadataKey.CATEGORY.name(),
                    value
                ));

        Optional.ofNullable(article.getPriority())
                .ifPresent(value -> metadata.put(
                    VectorStoreMetadataKey.PRIORITY.name(),
                    value
                ));

        Optional.ofNullable(article.getArticleType())
                .map(Enum::name)
                .ifPresent(value -> metadata.put(
                    VectorStoreMetadataKey.ARTICLE_TYPE.name(),
                    value
                ));

        Optional.ofNullable(article.getKeywords())
                .map(Arrays::asList)
                .ifPresent(value -> metadata.put(
                    VectorStoreMetadataKey.KEYWORDS.name(),
                    value
                ));

        return metadata;
    }

    private String renderContent(
        KnowledgeArticle article
    ) {
        return String.format(
            "%s%n%n%s",
            article.getTitle(),
            article.getContent()
        );
    }
}
