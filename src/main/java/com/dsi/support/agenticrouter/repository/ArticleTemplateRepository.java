package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.ArticleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleTemplateRepository extends JpaRepository<ArticleTemplate, Long> {

}
