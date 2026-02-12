package com.dsi.support.agenticrouter.service.knowledge;

import com.dsi.support.agenticrouter.configuration.RagConfiguration;
import com.dsi.support.agenticrouter.dto.ArticleSearchResult;
import com.dsi.support.agenticrouter.entity.KnowledgeArticle;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.VectorStoreMetadataKey;
import com.dsi.support.agenticrouter.repository.KnowledgeArticleRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeRetrievalService {

    private static final String SCORE_KEY = "score";
    private static final String SIMILARITY_KEY = "similarity";
    private static final String DISTANCE_KEY = "distance";

    private final KnowledgeBaseVectorStore knowledgeBaseVectorStore;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final ArticleRankingPolicy articleRankingPolicy;
    private final LibraryLexicalScorer libraryLexicalScorer;
    private final RagConfiguration ragConfiguration;

    public List<ArticleSearchResult> retrieveRelevantArticles(
            String query,
            TicketCategory categoryHint,
            int topK
    ) {
        log.debug(
                "KnowledgeRetrieve({}) Outcome(queryLength:{},categoryHint:{},topK:{})",
                OperationalLogContext.PHASE_START,
                StringUtils.length(query),
                Objects.nonNull(categoryHint) ? categoryHint : "NONE",
                topK
        );

        String normalizedQuery = StringUtils.trimToNull(
                query
        );

        if (StringUtils.isBlank(normalizedQuery) || topK <= 0) {
            log.debug(
                    "KnowledgeRetrieve({}) Outcome(reason:{})",
                    OperationalLogContext.PHASE_SKIP,
                    "empty_query_or_invalid_topk"
            );
            return List.of();
        }

        int denseCandidateLimit = calculateDenseCandidateLimit(
                topK
        );

        List<Document> documents = searchDenseCandidates(
                normalizedQuery,
                denseCandidateLimit
        );

        logDenseCandidateStats(
                denseCandidateLimit,
                documents
        );

        Map<Long, ArticleCandidate> candidates = reduceToBestCandidatePerArticle(
                documents
        );

        if (candidates.isEmpty()) {
            log.debug(
                    "KnowledgeRetrieve({}) Outcome(resultCount:{})",
                    OperationalLogContext.PHASE_COMPLETE,
                    0
            );
            return List.of();
        }

        String categoryHintText = Objects.isNull(categoryHint) ? null : categoryHint.name();

        Map<Long, KnowledgeArticle> articleById = loadArticles(
                candidates.keySet()
        );

        Map<Long, ArticleCandidate> scopedCandidates = scopeCandidatesByCategory(
                candidates,
                articleById,
                categoryHintText
        );

        List<ArticleSearchResult> results = rankAndLimitResults(
                scopedCandidates,
                articleById,
                normalizedQuery,
                categoryHintText,
                topK
        );

        log.debug(
                "KnowledgeRetrieve({}) Outcome(candidateCount:{},resultCount:{})",
                OperationalLogContext.PHASE_COMPLETE,
                scopedCandidates.size(),
                results.size()
        );

        return results;
    }

    private int calculateDenseCandidateLimit(
            int topK
    ) {
        return Math.clamp(
                topK,
                ragConfiguration.getDenseTopN(),
                Integer.MAX_VALUE
        );
    }

    private List<Document> searchDenseCandidates(
            String normalizedQuery,
            int denseCandidateLimit
    ) {
        return knowledgeBaseVectorStore.searchSimilar(
                normalizedQuery,
                denseCandidateLimit,
                ragConfiguration.getSimilarityThreshold()
        );
    }

    private void logDenseCandidateStats(
            int denseCandidateLimit,
            List<Document> documents
    ) {
        log.debug(
                "KnowledgeRetrieve({}) Outcome(denseCandidateLimit:{},rawDocumentCount:{},similarityThreshold:{})",
                OperationalLogContext.PHASE_DECISION,
                denseCandidateLimit,
                documents.size(),
                ragConfiguration.getSimilarityThreshold()
        );
    }

    private Map<Long, ArticleCandidate> reduceToBestCandidatePerArticle(
            List<Document> documents
    ) {
        Map<Long, ArticleCandidate> candidates = new HashMap<>();

        for (Document document : documents) {
            ArticleCandidate incoming = toCandidate(
                    document
            );
            if (Objects.isNull(incoming)) {
                continue;
            }

            ArticleCandidate existing = candidates.get(
                    incoming.getArticleId()
            );
            if (Objects.isNull(existing) || incoming.getScore() > existing.getScore()) {
                candidates.put(
                        incoming.getArticleId(),
                        incoming
                );
            }
        }

        return candidates;
    }

    private ArticleCandidate toCandidate(
            Document document
    ) {
        Long articleId = parseLong(metadata(
                document,
                VectorStoreMetadataKey.ARTICLE_ID.name()
        ));
        if (Objects.isNull(articleId)) {
            return null;
        }

        return ArticleCandidate.builder()
                .articleId(articleId)
                .score(extractVectorScore(
                        document
                ))
                .title(metadata(
                        document,
                        VectorStoreMetadataKey.TITLE.name()
                ))
                .category(metadata(
                        document,
                        VectorStoreMetadataKey.CATEGORY.name()
                ))
                .priority(parseInt(metadata(
                        document,
                        VectorStoreMetadataKey.PRIORITY.name()
                )))
                .articleType(metadata(
                        document,
                        VectorStoreMetadataKey.ARTICLE_TYPE.name()
                ))
                .build();
    }

    private List<ArticleSearchResult> rankAndLimitResults(
            Map<Long, ArticleCandidate> candidates,
            Map<Long, KnowledgeArticle> articleById,
            String query,
            String categoryHintText,
            int topK
    ) {
        return candidates.values()
                .stream()
                .map(
                        candidate -> toSearchResult(
                                candidate,
                                articleById.get(candidate.getArticleId()),
                                query,
                                categoryHintText
                        )
                )
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ArticleSearchResult::getSimilarityScore).reversed())
                .limit(topK)
                .toList();
    }

    private Map<Long, ArticleCandidate> scopeCandidatesByCategory(
            Map<Long, ArticleCandidate> candidates,
            Map<Long, KnowledgeArticle> articleById,
            String categoryHintText
    ) {
        if (StringUtils.isBlank(categoryHintText) || TicketCategory.OTHER.name().equalsIgnoreCase(categoryHintText)) {
            return candidates;
        }

        Map<Long, ArticleCandidate> scopedCandidates = candidates.entrySet()
                                                                 .stream()
                                                                 .filter(
                                                                     entry -> StringUtils.equalsIgnoreCase(
                                                                         resolveCategory(
                                                                             entry.getValue(),
                                                                             articleById.get(entry.getKey())
                                                                         ),
                                                                         categoryHintText
                                                                     )
                                                                 )
                                                                 .collect(Collectors.toMap(
                                                                     Map.Entry::getKey,
                                                                     Map.Entry::getValue
                                                                 ));

        if (scopedCandidates.isEmpty()) {
            log.debug(
                "KnowledgeRetrieve({}) Outcome(categoryHint:{},candidateCount:{},scopedCount:{},mode:{})",
                OperationalLogContext.PHASE_DECISION,
                categoryHintText,
                candidates.size(),
                0,
                "fallback_all"
            );
            return candidates;
        }

        log.debug(
            "KnowledgeRetrieve({}) Outcome(categoryHint:{},candidateCount:{},scopedCount:{},mode:{})",
            OperationalLogContext.PHASE_DECISION,
            categoryHintText,
            candidates.size(),
            scopedCandidates.size(),
            "scoped"
        );

        return scopedCandidates;
    }

    private Map<Long, KnowledgeArticle> loadArticles(
            Set<Long> articleIds
    ) {
        return knowledgeArticleRepository.findAllById(
                        articleIds
                )
                .stream()
                .collect(Collectors.toMap(
                        KnowledgeArticle::getId,
                        article -> article
                ));
    }

    private ArticleSearchResult toSearchResult(
            ArticleCandidate candidate,
            KnowledgeArticle article,
            String query,
            String categoryHint
    ) {
        if (Objects.isNull(candidate) || Objects.isNull(candidate.getArticleId())) {
            return null;
        }

        String category = resolveCategory(
                candidate,
                article
        );

        Integer priority = resolvePriority(
                candidate,
                article
        );

        String articleType = resolveArticleType(
                candidate,
                article
        );

        String title = resolveTitle(
                candidate,
                article
        );

        double successRate = Objects.isNull(article) ? 0D : article.getSuccessRate();
        double lexicalScore = libraryLexicalScorer.score(
            query,
            title,
            Objects.nonNull(article) ? article.getKeywords() : null
        );

        double rankedScore = articleRankingPolicy.rank(
                candidate.getScore(),
                lexicalScore,
                Objects.isNull(priority) ? 0 : priority,
                successRate,
                category,
                categoryHint
        );

        return ArticleSearchResult.builder()
                .articleId(candidate.getArticleId())
                .title(StringUtils.defaultIfBlank(
                        title,
                        "Unknown Title"
                ))
                .similarityScore(rankedScore)
                .category(category)
                .priority(Objects.isNull(priority) ? 0 : priority)
                .articleType(articleType)
                .contentPreview(buildContentPreview(article))
                .build();
    }

    private String buildContentPreview(
            KnowledgeArticle article
    ) {
        if (Objects.isNull(article)) {
            return null;
        }

        return StringUtils.abbreviate(
                StringUtils.normalizeSpace(
                        StringUtils.defaultString(article.getContent())
                ),
                280
        );
    }

    private String resolveCategory(
            ArticleCandidate candidate,
            KnowledgeArticle article
    ) {
        return firstNonBlank(
                candidate.getCategory(),
                Objects.nonNull(article) && Objects.nonNull(article.getCategory()) ? article.getCategory().name() : null
        );
    }

    private Integer resolvePriority(
            ArticleCandidate candidate,
            KnowledgeArticle article
    ) {
        Integer priority = candidate.getPriority();
        if (Objects.nonNull(priority)) {
            return priority;
        }

        return Objects.nonNull(article) ? article.getPriority() : null;
    }

    private String resolveArticleType(
            ArticleCandidate candidate,
            KnowledgeArticle article
    ) {
        return firstNonBlank(
                candidate.getArticleType(),
                Objects.nonNull(article) && Objects.nonNull(article.getArticleType()) ? article.getArticleType().name() : null
        );
    }

    private String resolveTitle(
            ArticleCandidate candidate,
            KnowledgeArticle article
    ) {
        return firstNonBlank(
                candidate.getTitle(),
                Objects.nonNull(article) ? article.getTitle() : null
        );
    }

    private String metadata(
            Document document,
            String key
    ) {
        if (Objects.isNull(document) || Objects.isNull(key)) {
            return null;
        }

        Map<String, Object> metadata = document.getMetadata();

        Object value = metadata.get(
                key
        );

        return Objects.isNull(value) ? null : value.toString();
    }

    private double extractVectorScore(
            Document document
    ) {
        Double score = parseDouble(metadata(
                document,
                SCORE_KEY
        ));
        if (Objects.nonNull(score)) {
            return clamp01(
                    score
            );
        }

        Double similarity = parseDouble(metadata(
                document,
                SIMILARITY_KEY
        ));
        if (Objects.nonNull(similarity)) {
            return clamp01(
                    similarity
            );
        }

        Double distance = parseDouble(metadata(
                document,
                DISTANCE_KEY
        ));
        if (Objects.nonNull(distance)) {
            double nonNegativeDistance = Math.clamp(
                    distance,
                    0D,
                    Double.MAX_VALUE
            );

            return clamp01(
                    1D - nonNegativeDistance
            );
        }

        return 0.5D;
    }

    private Long parseLong(
            String value
    ) {
        String normalized = StringUtils.trimToNull(
                value
        );
        if (Objects.isNull(normalized) || !StringUtils.isNumeric(
                normalized
        )) {
            return null;
        }

        return Long.parseLong(
                normalized
        );
    }

    private Integer parseInt(
            String value
    ) {
        String normalized = StringUtils.trimToNull(
                value
        );
        if (Objects.isNull(normalized) || !StringUtils.isNumeric(
                normalized
        )) {
            return null;
        }

        return Integer.parseInt(
                normalized
        );
    }

    private Double parseDouble(
            String value
    ) {
        String normalized = StringUtils.trimToNull(
                value
        );
        if (Objects.isNull(normalized)) {
            return null;
        }

        try {
            return Double.valueOf(
                    normalized
            );
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstNonBlank(
            String primary,
            String fallback
    ) {
        String normalizedPrimary = StringUtils.trimToNull(
                primary
        );
        if (Objects.nonNull(normalizedPrimary)) {
            return normalizedPrimary;
        }

        return StringUtils.trimToNull(
                fallback
        );
    }

    private double clamp01(
            double value
    ) {
        return Math.clamp(
                value,
                0D,
                1D
        );
    }

    @Getter
    @Builder
    private static class ArticleCandidate {
        private Long articleId;
        private double score;
        private String title;
        private String category;
        private Integer priority;
        private String articleType;
    }
}
