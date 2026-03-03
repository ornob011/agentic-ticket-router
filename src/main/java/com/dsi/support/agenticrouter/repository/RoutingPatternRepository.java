package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.RoutingPattern;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoutingPatternRepository extends JpaRepository<RoutingPattern, Long> {

    List<RoutingPattern> findByCategoryAndActiveTrueOrderBySuccessCountDesc(TicketCategory category);

    List<RoutingPattern> findBySuccessfulActionAndActiveTrueOrderBySuccessCountDesc(NextAction action);

    @Query("""
        SELECT p
        FROM RoutingPattern p
        WHERE p.active = true
        AND p.successCount >= :minSuccesses
        ORDER BY p.successCount DESC
        """)
    List<RoutingPattern> findReliablePatterns(@Param("minSuccesses") int minSuccesses);

    @Query("""
        SELECT p
        FROM RoutingPattern p
        WHERE p.category = :category
        AND p.successfulAction = :action
        AND p.active = true
        """)
    Optional<RoutingPattern> findByCategoryAndAction(
        @Param("category") TicketCategory category,
        @Param("action") NextAction action
    );

    @Query("""
        SELECT p
        FROM RoutingPattern p
        WHERE :keyword MEMBER OF p.keywords
          AND p.active = true
        ORDER BY p.successCount DESC
        """)
    List<RoutingPattern> findByKeyword(@Param("keyword") String keyword);

    @Query("""
        SELECT DISTINCT p FROM RoutingPattern p
        WHERE p.active = true
        AND EXISTS (SELECT 1 FROM p.keywords k WHERE k IN :keywords)
        ORDER BY p.successCount DESC
        """)
    List<RoutingPattern> findByKeywordsIn(
        @Param("keywords") List<String> keywords
    );

}
