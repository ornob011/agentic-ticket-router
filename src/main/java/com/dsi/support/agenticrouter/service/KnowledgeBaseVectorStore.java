package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.KnowledgeArticle;
import com.dsi.support.agenticrouter.enums.VectorStoreMetadataKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseVectorStore {

    private static final String DOCUMENT_ID_PREFIX = "kb_article_";
    private static final int REMOVE_ALL_TOP_K = 10000;

    private final VectorStore vectorStore;

    public List<Document> searchSimilar(
        String queryText,
        int topK,
        double similarityThreshold
    ) {
        Objects.requireNonNull(queryText, "queryText");

        SearchRequest searchRequest = buildSearchRequest(
            queryText,
            topK,
            similarityThreshold
        );

        return vectorStore.similaritySearch(searchRequest);
    }

    public void syncArticles(
        List<KnowledgeArticle> articles
    ) {
        Objects.requireNonNull(articles, "articles");

        log.info("Syncing {} articles to vector store", articles.size());

        List<Document> documents = articles.stream()
                                           .filter(article -> Objects.nonNull(article.getId()))
                                           .map(this::createDocument)
                                           .toList();

        if (documents.isEmpty()) {
            return;
        }

        vectorStore.add(documents);
        log.info("Synced {} documents to vector store", documents.size());
    }

    public void removeAll() {
        log.warn("Removing all documents from vector store");

        SearchRequest searchRequest = buildSearchRequest(
            StringUtils.EMPTY,
            REMOVE_ALL_TOP_K,
            0.0
        );

        List<String> allDocumentIds = vectorStore.similaritySearch(searchRequest)
                                                 .stream()
                                                 .map(Document::getId)
                                                 .toList();

        if (allDocumentIds.isEmpty()) {
            log.info("No documents found to remove from vector store");
            return;
        }

        vectorStore.delete(allDocumentIds);
        log.info("Removed {} documents from vector store", allDocumentIds.size());
    }

    private SearchRequest buildSearchRequest(
        String queryText,
        int topK,
        double similarityThreshold
    ) {
        return SearchRequest.builder()
                            .query(queryText)
                            .topK(topK)
                            .similarityThreshold(similarityThreshold)
                            .build();
    }

    private String getDocumentId(
        KnowledgeArticle article
    ) {
        return DOCUMENT_ID_PREFIX + article.getId();
    }

    private Document createDocument(
        KnowledgeArticle article
    ) {
        String documentId = getDocumentId(article);

        Map<String, Object> metadata = new HashMap<>();

        metadata.put(
            VectorStoreMetadataKey.ARTICLE_ID.name(),
            article.getId()
        );

        category(article).ifPresent(
            value -> metadata.put(
                VectorStoreMetadataKey.CATEGORY.name(),
                value
            )
        );

        priority(article).ifPresent(
            value -> metadata.put(
                VectorStoreMetadataKey.PRIORITY.name(),
                value
            )
        );

        articleType(article).ifPresent(
            value -> metadata.put(
                VectorStoreMetadataKey.ARTICLE_TYPE.name(),
                value
            )
        );

        metadata.put(
            VectorStoreMetadataKey.ACTIVE.name(),
            article.getActive()
        );

        keywords(article).ifPresent(
            value -> metadata.put(
                VectorStoreMetadataKey.KEYWORDS.name(),
                value
            )
        );

        return new Document(
            documentId,
            renderContent(article),
            metadata
        );
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

    private Optional<String> category(
        KnowledgeArticle article
    ) {
        return Optional.ofNullable(article.getCategory())
                       .map(Enum::name);
    }

    private Optional<Integer> priority(
        KnowledgeArticle article
    ) {
        return Optional.ofNullable(article.getPriority());
    }

    private Optional<String> articleType(
        KnowledgeArticle article
    ) {
        return Optional.ofNullable(article.getArticleType())
                       .map(Enum::name);
    }

    private Optional<List<String>> keywords(
        KnowledgeArticle article
    ) {
        return Optional.ofNullable(article.getKeywords())
                       .map(Arrays::asList);
    }
}
