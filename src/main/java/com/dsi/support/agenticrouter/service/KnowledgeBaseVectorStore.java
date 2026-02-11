package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.configuration.VectorStoreIngestionConfiguration;
import com.dsi.support.agenticrouter.entity.KnowledgeArticle;
import com.dsi.support.agenticrouter.enums.VectorStoreMetadataKey;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.DelegatingProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseVectorStore {

    private static final String DOCUMENT_ID_PREFIX = "kb_article_";

    private final VectorStore vectorStore;
    private final VectorStoreIngestionConfiguration vectorStoreIngestionConfiguration;

    public List<Document> searchSimilar(
        String queryText,
        int topK,
        double similarityThreshold
    ) {
        return searchSimilar(
            queryText,
            topK,
            similarityThreshold,
            activeFilter()
        );
    }

    public List<Document> searchSimilar(
        String queryText,
        int topK,
        double similarityThreshold,
        Filter.Expression filterExpression
    ) {
        log.debug(
            "VectorSearch({}) Outcome(queryLength:{},topK:{},similarityThreshold:{},hasFilter:{})",
            OperationalLogContext.PHASE_START,
            StringUtils.length(queryText),
            topK,
            similarityThreshold,
            Objects.nonNull(filterExpression)
        );

        Objects.requireNonNull(queryText, "queryText");

        SearchRequest searchRequest = buildSearchRequest(
            queryText,
            topK,
            similarityThreshold,
            filterExpression
        );

        List<Document> strictMatchDocuments = Optional.of(vectorStore.similaritySearch(searchRequest))
                                                      .orElse(Collections.emptyList());

        if (!strictMatchDocuments.isEmpty() || similarityThreshold <= 0D) {
            log.debug(
                "VectorSearch({}) Outcome(resultCount:{},mode:{})",
                OperationalLogContext.PHASE_COMPLETE,
                strictMatchDocuments.size(),
                "strict"
            );

            return strictMatchDocuments;
        }

        SearchRequest relaxedSearchRequest = buildSearchRequest(
            queryText,
            topK,
            0D,
            filterExpression
        );

        List<Document> relaxedMatchDocuments = Optional.of(vectorStore.similaritySearch(relaxedSearchRequest))
                                                       .orElse(Collections.emptyList());

        if (!relaxedMatchDocuments.isEmpty()) {
            log.debug(
                "VectorSearch({}) Outcome(strictThreshold:{},resultCount:{},mode:{})",
                OperationalLogContext.PHASE_DECISION,
                similarityThreshold,
                relaxedMatchDocuments.size(),
                "relaxed"
            );
        }

        log.debug(
            "VectorSearch({}) Outcome(resultCount:{},mode:{})",
            OperationalLogContext.PHASE_COMPLETE,
            relaxedMatchDocuments.size(),
            "relaxed"
        );

        return relaxedMatchDocuments;
    }

    public void syncArticles(
        List<KnowledgeArticle> knowledgeArticles
    ) {
        Objects.requireNonNull(knowledgeArticles, "knowledgeArticles list cannot be null");

        if (knowledgeArticles.isEmpty()) {
            log.debug(
                "VectorSync({}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                "no_articles"
            );
            return;
        }

        log.info(
            "VectorSync({}) Outcome(articleCount:{},batchSize:{})",
            OperationalLogContext.PHASE_START,
            knowledgeArticles.size(),
            vectorStoreIngestionConfiguration.getIngestBatchSize()
        );

        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                                                               .withChunkSize(vectorStoreIngestionConfiguration.getSplitChunkSize())
                                                               .withMinChunkSizeChars(vectorStoreIngestionConfiguration.getSplitMinChunkSizeChars())
                                                               .withMinChunkLengthToEmbed(vectorStoreIngestionConfiguration.getSplitMinChunkLengthToEmbed())
                                                               .withKeepSeparator(false)
                                                               .build();

        ProgressBarBuilder progressBarBuilder = new ProgressBarBuilder().setTaskName("Sync articles")
                                                                        .setInitialMax(knowledgeArticles.size())
                                                                        .setUpdateIntervalMillis(vectorStoreIngestionConfiguration.getProgressUpdateIntervalMs())
                                                                        .setStyle(ProgressBarStyle.ASCII)
                                                                        .setConsumer(new DelegatingProgressBarConsumer(log::info));

        List<Document> ingestBatch = new ArrayList<>(vectorStoreIngestionConfiguration.getIngestBatchSize());
        int syncedDocuments = 0;

        for (KnowledgeArticle knowledgeArticle : ProgressBar.wrap(knowledgeArticles, progressBarBuilder)) {
            if (Objects.isNull(knowledgeArticle.getId())) {
                continue;
            }

            ingestBatch.add(
                createDocument(knowledgeArticle)
            );

            if (ingestBatch.size() >= vectorStoreIngestionConfiguration.getIngestBatchSize()) {
                List<Document> chunks = tokenTextSplitter.apply(ingestBatch);

                vectorStore.add(chunks);

                syncedDocuments += chunks.size();

                ingestBatch.clear();
            }
        }

        if (!ingestBatch.isEmpty()) {
            List<Document> chunks = tokenTextSplitter.apply(ingestBatch);

            vectorStore.add(chunks);

            syncedDocuments += chunks.size();

            ingestBatch.clear();
        }

        log.info(
            "VectorSync({}) Outcome(chunkDocumentCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            syncedDocuments
        );
    }

    public void removeAll() {
        log.warn(
            "VectorRemoveAll({})",
            OperationalLogContext.PHASE_START
        );

        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        Filter.Expression filterExpression = filterExpressionBuilder.gte(
            VectorStoreMetadataKey.ARTICLE_ID.name(),
            0L
        ).build();

        vectorStore.delete(filterExpression);

        log.info(
            "VectorRemoveAll({}) Outcome(status:{})",
            OperationalLogContext.PHASE_COMPLETE,
            "success"
        );
    }

    private SearchRequest buildSearchRequest(
        String queryText,
        int topK,
        double similarityThreshold,
        Filter.Expression filterExpression
    ) {
        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                                                                  .query(queryText)
                                                                  .topK(topK)
                                                                  .similarityThreshold(similarityThreshold);

        if (Objects.nonNull(filterExpression)) {
            searchRequestBuilder.filterExpression(filterExpression);
        }

        return searchRequestBuilder.build();
    }

    private Filter.Expression activeFilter() {
        return new FilterExpressionBuilder().eq(
            VectorStoreMetadataKey.ACTIVE.name(),
            true
        ).build();
    }

    private String getDocumentId(
        KnowledgeArticle article
    ) {
        String sourceId = DOCUMENT_ID_PREFIX + article.getId();

        return UUID.nameUUIDFromBytes(sourceId.getBytes(StandardCharsets.UTF_8))
                   .toString();
    }

    private Document createDocument(
        KnowledgeArticle article
    ) {
        String documentId = getDocumentId(article);

        Map<String, Object> metadata = HashMap.newHashMap(8);

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
