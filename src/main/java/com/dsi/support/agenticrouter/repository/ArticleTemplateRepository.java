package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.ArticleTemplate;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleTemplateRepository extends JpaRepository<ArticleTemplate, Long> {

    @Query("""
            SELECT DISTINCT template
            FROM ArticleTemplate template
            JOIN template.applicableCategories category
            JOIN template.applicablePriorities priority
            WHERE template.active = true
              AND category = :category
              AND priority = :priority
            ORDER BY template.name ASC
        """)
    List<ArticleTemplate> findByCategoryAndPriority(
        @Param("category") TicketCategory category,
        @Param("priority") TicketPriority priority
    );

    @Query("""
            SELECT DISTINCT template
            FROM ArticleTemplate template
            JOIN template.applicableCategories category
            JOIN template.applicablePriorities priority
            WHERE template.active = true
              AND category = :category
              AND priority = :priority
              AND SIZE(template.applicableCategories) = 1
              AND SIZE(template.applicablePriorities) = 1
            ORDER BY template.name ASC
        """)
    List<ArticleTemplate> findByCategoryAndPriorityWithExactMatch(
        @Param("category") TicketCategory category,
        @Param("priority") TicketPriority priority
    );

    @Query("""
            SELECT DISTINCT template
            FROM ArticleTemplate template
            JOIN template.applicableCategories category
            WHERE template.active = true
              AND category = :category
            ORDER BY template.name ASC
        """)
    List<ArticleTemplate> findByCategoryOnly(@Param("category") TicketCategory category);

    @Query("""
            SELECT DISTINCT template
            FROM ArticleTemplate template
            JOIN template.applicablePriorities priority
            WHERE template.active = true
              AND priority = :priority
            ORDER BY template.name ASC
        """)
    List<ArticleTemplate> findByPriorityOnly(@Param("priority") TicketPriority priority);

    @Query("""
            SELECT template
            FROM ArticleTemplate template
            WHERE template.active = true
              AND SIZE(template.applicableCategories) = 0
              AND SIZE(template.applicablePriorities) = 0
            ORDER BY template.name ASC
        """)
    List<ArticleTemplate> findGlobalTemplates();
}
