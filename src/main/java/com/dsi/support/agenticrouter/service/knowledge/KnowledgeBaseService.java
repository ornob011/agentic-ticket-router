package com.dsi.support.agenticrouter.service.knowledge;

import com.dsi.support.agenticrouter.entity.KnowledgeArticle;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.KnowledgeArticleRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseService {

    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final KnowledgeChunkIngestionService knowledgeChunkIngestionService;

    @Transactional
    public void recordUsage(
        Long articleId,
        boolean success
    ) {
        log.debug(
            "KnowledgeUsageRecord({}) KnowledgeArticle(id:{}) Outcome(success:{})",
            OperationalLogContext.PHASE_START,
            articleId,
            success
        );

        Objects.requireNonNull(articleId, "articleId");

        KnowledgeArticle article = knowledgeArticleRepository.findById(articleId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     KnowledgeArticle.class,
                                                                     articleId
                                                                 )
                                                             );

        article.incrementUsage();

        if (success) {
            article.incrementSuccess();
        } else {
            article.incrementFailure();
        }

        knowledgeArticleRepository.save(article);

        log.debug(
            "KnowledgeUsageRecord({}) KnowledgeArticle(id:{},usageCount:{}) Outcome(successCount:{},failureCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            article.getId(),
            article.getUsageCount(),
            article.getSuccessCount(),
            article.getFailureCount()
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void initializeVectorStore() {
        log.info(
            "VectorStoreInitialization({})",
            OperationalLogContext.PHASE_START
        );

        List<KnowledgeArticle> knowledgeArticles = knowledgeArticleRepository.findAllByActiveTrueOrderByPriorityDesc();

        knowledgeChunkIngestionService.build(
            knowledgeArticles
        );

        log.info(
            "VectorStoreInitialization({}) Outcome(knowledgeArticleCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            knowledgeArticles.size()
        );
    }

}
