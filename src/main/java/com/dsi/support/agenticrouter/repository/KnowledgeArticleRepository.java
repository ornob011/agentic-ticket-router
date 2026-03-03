package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    List<KnowledgeArticle> findAllByActiveTrueOrderByPriorityDesc();
}
