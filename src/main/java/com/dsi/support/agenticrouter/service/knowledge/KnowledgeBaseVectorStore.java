package com.dsi.support.agenticrouter.service.knowledge;

import com.dsi.support.agenticrouter.enums.VectorStoreMetadataKey;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseVectorStore {

    private final VectorStore vectorStore;

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

        log.debug(
            "VectorSearch({}) Outcome(resultCount:{},mode:{})",
            OperationalLogContext.PHASE_COMPLETE,
            0,
            "strict"
        );

        return Collections.emptyList();
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

    public void deleteByArticleId(
        Long articleId
    ) {
        Objects.requireNonNull(articleId, "articleId");

        Filter.Expression filterExpression = new FilterExpressionBuilder().eq(
            VectorStoreMetadataKey.ARTICLE_ID.name(),
            articleId
        ).build();

        vectorStore.delete(filterExpression);
    }

    public void addDocuments(
        List<Document> documents
    ) {
        Objects.requireNonNull(documents, "documents");

        if (documents.isEmpty()) {
            return;
        }

        vectorStore.add(documents);
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
