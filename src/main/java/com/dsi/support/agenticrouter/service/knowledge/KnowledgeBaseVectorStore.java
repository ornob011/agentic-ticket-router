package com.dsi.support.agenticrouter.service.knowledge;

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

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseVectorStore {

    private final VectorStore vectorStore;
    private final VectorStoreIngestionConfiguration vectorStoreIngestionConfiguration;
    private final KnowledgeArticleVectorDocumentMapper knowledgeArticleVectorDocumentMapper;

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

        List<Document> strictMatchDocuments = Optional.ofNullable(vectorStore.similaritySearch(searchRequest))
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

        log.debug(
            "VectorSearch({}) Outcome(resultCount:{},mode:{})",
            OperationalLogContext.PHASE_COMPLETE,
            0,
            "strict"
        );

        return Collections.emptyList();
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
                knowledgeArticleVectorDocumentMapper.toDocument(knowledgeArticle)
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

}
