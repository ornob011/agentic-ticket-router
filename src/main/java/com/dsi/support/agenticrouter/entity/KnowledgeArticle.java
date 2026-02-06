package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.ArticleType;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;

@Entity
@Table(name = "knowledge_article")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticle extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketCategory category;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]")
    private String[] keywords;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ArticleType articleType = ArticleType.SOLUTION;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Long usageCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long successCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long failureCount = 0L;

    public void incrementUsage() {
        usageCount = Objects.requireNonNullElse(usageCount, 0L) + 1L;
    }

    public void incrementSuccess() {
        successCount = Objects.requireNonNullElse(successCount, 0L) + 1L;
    }

    public void incrementFailure() {
        failureCount = Objects.requireNonNullElse(failureCount, 0L) + 1L;
    }

    public Double getSuccessRate() {
        long uses = Objects.requireNonNullElse(usageCount, 0L);
        long successes = Objects.requireNonNullElse(successCount, 0L);

        if (uses == 0L) {
            return 0.0;
        }

        return (double) successes / (double) uses;
    }
}
