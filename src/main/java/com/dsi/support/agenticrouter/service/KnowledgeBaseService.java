package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.KnowledgeArticle;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseService {

    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final KnowledgeBaseVectorStore knowledgeBaseVectorStore;

    @Transactional
    public void recordUsage(
        Long articleId,
        boolean success
    ) {
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
    }

    @Transactional
    public void initializeVectorStore() {
        log.info("Initializing vector store with existing articles");

        List<KnowledgeArticle> articles = knowledgeArticleRepository.findAllByActiveTrueOrderByPriorityDesc();

        knowledgeBaseVectorStore.removeAll();
        knowledgeBaseVectorStore.syncArticles(articles);

        log.info("Vector store initialization complete with {} articles", articles.size());
    }
}
